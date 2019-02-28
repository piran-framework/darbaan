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

import com.piranframework.darbaan.model.Server;
import com.piranframework.darbaan.model.Service;
import com.piranframework.geev.Geev;
import com.piranframework.geev.GeevConfig;
import com.piranframework.geev.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZFrame;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.piranframework.darbaan.Darbaan.configuration;
import static com.piranframework.darbaan.util.Constants.*;
import static com.piranframework.darbaan.util.IdentityUtil.serverId;
import static com.piranframework.darbaan.util.IdentityUtil.serviceId;

/**
 * Service pool indicate the state of the proximity nodes. By the use of Geev it can discover new
 * node and notify disconnected node on runtime without any special configuration.
 * <p>
 * Constructor accept a Consumer function to run when a new node discover. So this class can
 * notify its owner about new nodes
 *
 * @author Isa Hekmatizadeh
 */
class ServicePool {
  private static final Logger log = LoggerFactory.getLogger(ServicePool.class);
  private final Map<String, Service> services = new ConcurrentHashMap<>();
  private final Map<ZFrame, Server> servers = new ConcurrentHashMap<>();
  private final Geev geev;
  private final Consumer<String> registerNewNode;
  private final Consumer<Node> registerNewAdmin;
  private final Consumer<Node> unregisterAdmin;

  /**
   * Start geev node discovery based on the {@link DarbaanConfiguration}
   *
   * @param registerNewNode consumer to run after a new node found
   * @throws IOException if geev couldn't open socket
   */
  ServicePool(Consumer<String> registerNewNode, Consumer<Node> registerNewAdmin,
              Consumer<Node> unregisterAdmin) throws
    IOException {
    this.registerNewNode = registerNewNode;
    this.registerNewAdmin = registerNewAdmin;
    this.unregisterAdmin = unregisterAdmin;
    Node mySelf = new Node(CHANNEL_ROLE, configuration.getIp(), configuration.getPort());
    geev = Geev.run(new GeevConfig.Builder()
      .setMySelf(mySelf)
      .onJoin(this::join)
      .onLeave(this::leave)
      .build());
  }

  /**
   * Destroy geev gracefully
   */
  void destroy() {
    geev.destroy();
  }

  void notifyRemove(String id) {
    StringTokenizer st = new StringTokenizer(id, ":");
    Node node = new Node(SERVER_ROLE, st.nextToken(), Integer.parseInt(st.nextToken()));
    geev.nodeDisconnected(node);
    leave(node);
  }

  private void leave(Node node) {
    log.info("node {} left", node);
    if (ADMIN_ROLE.equals(node.getRole()))
      unregisterAdmin.accept(node);
    else if (SERVER_ROLE.equals(node.getRole())) {
      Server server = servers.remove(new ZFrame(serverId(node)));
      if (Objects.nonNull(server)) {
        server.getServices().stream().map(Service::id).forEach(services::remove);
        server.destroy();
      }
    }
  }

  private void join(Node node) {
    log.info("new node joined: {}", node);
    if (node.getRole().equals(SERVER_ROLE))
      registerNewNode.accept(serverId(node));
    else if (node.getRole().equals(ADMIN_ROLE))
      registerNewAdmin.accept(node);
  }

  void addService(ZFrame serverIdentity, Service service) {
    log.info("new service {} found in server {}", service.id(), serverIdentity);
    Server server = servers.get(serverIdentity);
    Service oldService = services.get(service.id());
    if (Objects.isNull(oldService)) {
      if (Objects.isNull(server)) {
        server = new Server(serverIdentity);
        servers.put(serverIdentity, server);
      }
      server.add(service);
      services.put(service.id(), service);
    } else {
      if (Objects.isNull(server)) {
        server = new Server(serverIdentity);
        servers.put(serverIdentity, server);
      }
      server.add(oldService);
    }
  }

  /**
   * Record an interaction with a server
   *
   * @param serverIdentity server identity
   */
  void interaction(ZFrame serverIdentity) {
    Server server = servers.get(serverIdentity);
    if (Objects.nonNull(server))
      server.interaction();
  }

  /**
   * Find and return servers which is inactive for a while
   *
   * @return inactiveServers
   */
  Stream<Server> findInactiveServers() {
    return servers.values().stream().filter(Server::notResponding);
  }

  /**
   * It's pick a next server to send a request for special service. it act in round-robin fashion
   *
   * @param serviceId service the server should provide
   * @return next server available to provide this service
   */
  ZFrame nextServer(String serviceId) {
    Service service = services.get(serviceId);
    if (Objects.isNull(service))
      return null;
    Server server = service.nextServer();
    if (Objects.isNull(server))
      return null;
    return server.getIdentity();
  }

  /**
   * Check if a specific service recognized in darbaan
   *
   * @param name    name of service
   * @param version version of service
   * @return true if service recognized
   */
  boolean isServiceAvailable(String name, String version) {
    return services.containsKey(serviceId(name, version));
  }
}
