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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * This class is responsible to communicate with Dastoor, when Geev found Dastoor node invoke
 * {@link this#join(Node)} method, this method is responsible to create a new thread and Dealer
 * socket to communicate with Dastoor, detail about the protocol of the communication documented
 * in PIRAN/DASTOOR specification.
 *
 * @author Isa Hekmatizadeh
 */
class AdminClient {

  private static final Logger log = LoggerFactory.getLogger(AdminClient.class);
  private final ZContext ctx;
  private final Map<Node, Thread> threads = new ConcurrentHashMap<>();
  private final Map<Node, Process> processes = new ConcurrentHashMap<>();
  private final BiConsumer<String, List<String>> addPermission;

  AdminClient(ZContext ctx, BiConsumer<String, List<String>> addPermission) {
    this.ctx = ctx;
    this.addPermission = addPermission;
  }

  /**
   * create a dedicated thread to handle communication with newly found ADMIN node
   *
   * @param adminServer newly found admin node
   */
  void join(Node adminServer) {
    if (threads.containsKey(adminServer))
      return;
    Process process = new Process(adminServer);
    Thread thread = new Thread(process);
    thread.start();
    threads.put(adminServer, thread);
    processes.put(adminServer, process);
  }

  /**
   * interrupt dedicated thread of this node.
   *
   * @param adminServer disconnected admin node
   */
  void leave(Node adminServer) {
    Thread thread = threads.remove(adminServer);
    Process process = processes.remove(adminServer);
    process.terminate();
    if (Objects.nonNull(thread)) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        thread.interrupt();
      }
    }
  }

  class Process implements Runnable {

    private final Node node;
    private volatile boolean stopped = false;
    private long lastSendHLT = 0;

    Process(Node node) {
      this.node = node;
    }

    void terminate() {
      stopped = true;
    }

    @Override
    public void run() {
      try (ZMQ.Socket dealer = ctx.createSocket(ZMQ.DEALER)) {
        dealer.setIdentity((Darbaan.configuration.getIp() + ":" + Darbaan.configuration.getPort()).getBytes());
        dealer.setReconnectIVLMax(1000);
        dealer.setSndHWM(1000);
        dealer.setRcvHWM(1000);
        dealer.connect(String.format("tcp://%s:%d", node.getIp(), node.getPort()));
        sendSecReq(dealer);
        while (!stopped) {
          try {
            sendHLT(dealer);
            handleRecv(dealer);
          } catch (ZMQException e) {
            if (ZError.ETERM == e.getErrorCode())
              break;
            else {
              log.error("Unexpected error occurred: ", e);
            }
          }
        }
      }
    }

    private void handleRecv(ZMQ.Socket dealer) {
      ZMsg msg = ZMsg.recvMsg(dealer, ZMQ.NOBLOCK);
      if (Objects.isNull(msg))
        return;
      msg.pop();//empty frame
      ZFrame protocol = msg.pop();
      if (!protocol.streq(Constants.DST_PROTOCOL_HEADER)) { //message isn't DST version 1
        log.error("corrupted message received: protocol frame is {}, rest of message is: {}",
            protocol, msg);
        return;
      }
      String command = msg.popString();
      if (Objects.equals(Constants.PERMS, command)) {
        handlePerms(msg);
      }
    }

    private void handlePerms(ZMsg msg) {
      String actionAddress = msg.popString();
      while (Objects.nonNull(actionAddress)) {
        String roles = msg.popString();
        List<String> listOfRoles = Arrays.asList(roles.split("/"));
        addPermission.accept(actionAddress, listOfRoles);
        actionAddress = msg.popString();
      }
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private void sendSecReq(ZMQ.Socket dealer) {
      ZMsg msg = new ZMsg();
      msg.add(Constants.DST_PROTOCOL_HEADER);
      msg.add(Constants.SEC_REQ);
      msg.send(dealer);
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private void sendHLT(ZMQ.Socket dealer) {
      if (lastSendHLT < System.currentTimeMillis() - Constants.HLT_INTERVAL) {
        ZMsg msg = new ZMsg();
        msg.add(Constants.DST_PROTOCOL_HEADER);
        msg.add(Constants.HLT);
        msg.add(Constants.CHANNEL_ROLE);
        msg.send(dealer);
        lastSendHLT = System.currentTimeMillis();
      }
    }
  }
}
