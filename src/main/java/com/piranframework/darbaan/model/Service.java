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

package com.piranframework.darbaan.model;

import com.piranframework.darbaan.util.IdentityUtil;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Represent a service which multiple servers provide.
 *
 * @author Isa Hekmatizadeh
 */
public class Service {

  private final Queue<Server> servers = new ConcurrentLinkedQueue<>();
  private final String id;

  /**
   * Construct a service with the given name and version
   *
   * @param name    service name
   * @param version service version
   */
  public Service(String name, String version) {
    this.id = IdentityUtil.serviceId(name, version);
  }

  /**
   * Get the service id
   *
   * @return service id
   */
  public String id() {
    return id;
  }

  /**
   * Add a server to the list of servers which provide this service
   *
   * @param server server to add
   */
  void justAdd(Server server) {
    synchronized (servers) {
      servers.add(server);
    }
  }

  /**
   * Remove a server from the list of servers which provide this service
   *
   * @param server server to remove
   */
  void justRemove(Server server) {
    synchronized (servers) {
      servers.remove(server);
    }
  }

  /**
   * Return the next server to call for this service. This method act as a round-robin load balancer
   *
   * @return next server to use
   */
  public Server nextServer() {
    Server next = servers.peek();
    if (Objects.isNull(next))
      return null;
    servers.add(next);
    servers.poll();
    return next;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Service service = (Service) o;
    return Objects.equals(id, service.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

}
