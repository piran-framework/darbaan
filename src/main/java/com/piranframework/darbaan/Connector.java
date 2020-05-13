/*
 *  Copyright (c) 2018 Isa Hekmatizadeh.
 *
 *  This file is part of Darbaan.
 *
 *  Darbaan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Darbaan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Darbaan.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.piranframework.darbaan;

import com.piranframework.darbaan.exception.RoleHasNotPermissionException;
import com.piranframework.darbaan.exception.UnknownServiceException;
import com.piranframework.darbaan.model.Service;
import com.piranframework.darbaan.util.Constants;
import com.piranframework.geev.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import org.zeromq.ZMsg;
import zmq.ZError;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.piranframework.darbaan.Darbaan.configuration;
import static com.piranframework.darbaan.util.Constants.CHANNEL_ROLE;
import static com.piranframework.darbaan.util.Constants.INTR;
import static com.piranframework.darbaan.util.Constants.PING;
import static com.piranframework.darbaan.util.Constants.PONG;
import static com.piranframework.darbaan.util.Constants.PROTOCOL_HEADER;
import static com.piranframework.darbaan.util.Constants.REP;
import static com.piranframework.darbaan.util.Constants.RINTR;
import static com.piranframework.darbaan.util.IdentityUtil.serverId;
import static com.piranframework.darbaan.util.IdentityUtil.serviceId;
import static org.zeromq.ZMsg.recvMsg;

/**
 * This class is responsible to manage the zeroMQ socket and do the low level tasks
 *
 * @author Isa Hekmatizadeh
 */
class Connector {

  private static final Logger log = LoggerFactory.getLogger(Connector.class);
  private final Thread internalThread;
  private final Thread monitorThread;
  private final ZContext ctx;
  private final Consumer<Response> responseFn;
  private final PermissionCache permissionCache = new PermissionCache();
  private final Queue<String> newServers = new ConcurrentLinkedQueue<>();
  private final ServicePool servicePool;
  private final BlockingQueue<ZMsg> sendQueue = new LinkedBlockingQueue<>();
  private final Queue<ZFrame> pingQueue = new ConcurrentLinkedQueue<>();
  private final ExecutorService executorService = Executors.newFixedThreadPool(4);
  private ZMQ.Socket router;

  Connector(Consumer<Response> responseFn) throws IOException {
    this.responseFn = responseFn;
    this.ctx = new ZContext(1);
    AdminClient adminClient = new AdminClient(ctx, permissionCache::addPermission);
    servicePool = new ServicePool(newServers::add, adminClient::join, adminClient::leave);
    internalThread = new Thread(this::initialize);
    internalThread.setName("darbaan-socket-thread");
    internalThread.start();
    monitorThread = new Thread(this::monitor);
    monitorThread.setName("darbaan-monitor-thread");
    monitorThread.start();
  }

  private static StringBuffer msgDump(ZMsg msg) {
    StringBuffer msgDump = new StringBuffer();
    if (Objects.nonNull(msg))
      msg.dump(msgDump);
    return msgDump;
  }

  private void monitor() {
    while (!Thread.currentThread().isInterrupted()) {
      servicePool.findInactiveServers().forEach(s -> {
        if (!s.retryPing())
          servicePool.notifyRemove(s.getIdentity().toString());
        else
          pingQueue.add(s.getIdentity());
      });
      try {
        // FIXME: Tricky - sleep in a loop
        Thread.sleep(Constants.PING_INTERVAL);
      } catch (InterruptedException e) {
        break;
      }
    }
  }

  private void initialize() {
    router = ctx.createSocket(ZMQ.ROUTER);
    Node node = new Node(CHANNEL_ROLE, configuration.getIp(), configuration.getPort());
    router.setIdentity(serverId(node).getBytes());
    router.setSndHWM(10000);
    router.setRcvHWM(10000);
    router.setRouterMandatory(true);
    String endpoint = "tcp://*:" + configuration.getPort();
    router.bind(endpoint);
    log.info("socket successfully bind to port {}", configuration.getPort());
    workLoop();
  }

  private void workLoop() {
    while (!Thread.currentThread().isInterrupted()) {
      handleNewNode();
      ZMsg shouldSend = null;
      try {
        shouldSend = sendQueue.poll(1, TimeUnit.MILLISECONDS);
        if (Objects.nonNull(shouldSend))
          shouldSend.send(router, false);
        ZMsg msg = recvMsg(router, ZMQ.NOBLOCK);
        executorService.submit(() -> handleReceive(msg));
        ZFrame shouldPing = pingQueue.poll();
        if (Objects.nonNull(shouldPing))
          sendPing(shouldPing);
      } catch (ZError.IOException e) {
        log.warn("Darbaan socket closed by interrupt");
      } catch (ZMQException e) {
        if (ZError.EHOSTUNREACH == e.getErrorCode()) {
          log.error("ERROR: host not found for this message:\n {}", msgDump(shouldSend));
        } else if (ZError.ETERM == e.getErrorCode()) {
          log.info("exited by termination");
          break;
        } else
          log.error("Unknown error:", e);
      } catch (InterruptedException e) {
        break;
      }
    }
  }

  private void sendPing(ZFrame serverIdentity) {
    try {
      //noinspection MismatchedQueryAndUpdateOfCollection
      ZMsg m = new ZMsg();
      m.add(PROTOCOL_HEADER);
      m.add(PING);
      m.wrap(serverIdentity);
      m.send(router);
    } catch (ZMQException e) {
      if (ZError.EHOSTUNREACH != e.getErrorCode())
        e.printStackTrace();
    }
  }

  private void handleReceive(ZMsg msg) {
    try {
      if (Objects.isNull(msg))
        return;
      if (log.isDebugEnabled())
        log.debug("new message arrived: {}", msgDump(msg));
      ZFrame serverIdentity = msg.unwrap();
      String protocol = msg.popString();
      if (checkProtocolHeader(serverIdentity, protocol, msg))
        return;
      String command = msg.popString();
      switch (command) {
        case INTR:
          handleIntroduce(serverIdentity, msg);
          break;
        case PONG:
          servicePool.interaction(serverIdentity);
          break;
        case REP:
          handleReply(serverIdentity, msg);
          break;
        default:
          log.error("Error: Unknown message received with command {}", command);
      }
    } catch (Exception e) {
      log.error("Unknown exception happened:", e);
    }
  }

  private void handleReply(ZFrame serverIdentity, ZMsg msg) {
    servicePool.interaction(serverIdentity);
    Response response = new Response(
        msg.popString(),
        ByteBuffer.wrap(msg.pop().getData()).getInt(),
        msg.pop().getData());
    responseFn.accept(response);
  }

  private void handleIntroduce(ZFrame serverIdentity, ZMsg msg) {
    String serviceName = msg.popString();
    while (Objects.nonNull(serviceName)) {
      String version = msg.popString();
      Service service = new Service(serviceName, version);
      servicePool.addService(serverIdentity, service);
      serviceName = msg.popString();
    }
    servicePool.interaction(serverIdentity);
  }

  /**
   * Check if the protocol frame is correct and log if not
   *
   * @param serverIdentity server which this message come from
   * @param protocol       protocol frame
   * @param msg            actual message
   * @return true if any problem rise and false if protocol frame is correct
   */
  private boolean checkProtocolHeader(ZFrame serverIdentity, String protocol, ZMsg msg) {
    if (!PROTOCOL_HEADER.equals(protocol)) {
      log.error("ERROR: bad message received from {} with protocol header {},: {}",
          serverIdentity, protocol, msgDump(msg));
      return true;
    }
    return false;
  }

  /**
   * Check if a new node discovered and send RINTR message to it
   */
  private void handleNewNode() {
    String serverId = newServers.poll();
    if (Objects.isNull(serverId))
      return;
    try {
      //noinspection MismatchedQueryAndUpdateOfCollection
      ZMsg m = new ZMsg();
      m.add(PROTOCOL_HEADER);
      m.add(RINTR);
      m.wrap(new ZFrame(serverId));
      m.send(router);
    } catch (ZMQException e) {
      if (e.getErrorCode() == ZError.EHOSTUNREACH)
        newServers.add(serverId); //retry it later
      else
        log.error("Error occurred while sending RINTR to {}:", serverId, e);
    }
  }

  /**
   * Create a zeroMQ message from request and put it to outbox queue
   *
   * @param request request to send
   * @throws UnknownServiceException if ordered service is unknown and not yet recognized
   */
  void send(Request request) throws UnknownServiceException, RoleHasNotPermissionException {
    String id = serviceId(request.getServiceName(), request.getServiceVersion());
    if (!permissionCache.hasAccess(id, request.getActionCategory(), request.getActionName(),
        request.getRole()))
      throw new RoleHasNotPermissionException(request.getRole(), id, request.getActionCategory(),
          request.getActionName());
    ZFrame serverFrame = servicePool.nextServer(id);
    if (Objects.isNull(serverFrame))
      throw new UnknownServiceException(request);
    ZMsg msg = new ZMsg();
    msg.add(Constants.PROTOCOL_HEADER);
    msg.add(Constants.REQ);
    msg.add(request.getRequestId());
    msg.add(request.getServiceName());
    msg.add(request.getServiceVersion());
    msg.add(request.getActionCategory());
    msg.add(request.getActionName());
    msg.add(request.getPayloadBytes());
    msg.wrap(serverFrame);
    sendQueue.add(msg);
  }

  /**
   * Check if a service is recognized
   *
   * @param name    service name
   * @param version service version
   * @return true if service is recognized
   */
  boolean isServiceAvailable(String name, String version) {
    return servicePool.isServiceAvailable(name, version);
  }

  /**
   * Destroy object gracefully
   */
  void destroy() {
    internalThread.interrupt();
    monitorThread.interrupt();
    try {
      executorService.awaitTermination(1, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    executorService.shutdown();
    servicePool.destroy();
    ctx.destroy();
  }
}
