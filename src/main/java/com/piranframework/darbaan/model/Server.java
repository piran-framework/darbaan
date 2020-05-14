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

import com.piranframework.darbaan.util.Constants;
import org.zeromq.ZFrame;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a server node, in Safir-Darbaan protocol this node should be a Safir node
 *
 * @author Isa Hekmatizadeh
 */
public class Server {

  private final ZFrame identity;
  private final List<Service> services = new ArrayList<>();
  private volatile long lastInteract = System.currentTimeMillis();
  private volatile int remainInterval = Constants.PING_RETRY;

  /**
   * Construct a Server with the given identity
   *
   * @param identity identity frame of the server
   */
  public Server(ZFrame identity) {
    this.identity = identity.duplicate();
  }

  public ZFrame getIdentity() {
    return identity.duplicate();
  }


  public long getLastInteract() {
    return lastInteract;
  }

  public Server setLastInteract(long lastInteract) {
    this.lastInteract = lastInteract;
    return this;
  }

  public List<Service> getServices() {
    return services;
  }

  /**
   * Add This Server to the service providers and add this service into services list of the server
   *
   * @param service service to add
   */
  public void add(Service service) {
    service.justAdd(this);
    services.add(service);
  }

  /**
   * Add this service into services list of the server
   *
   * @param service service to add
   */
  void justAdd(Service service) {
    services.add(service);
  }

  public void remove(Service service) {
    service.justRemove(this);
    services.remove(service);
  }

  void justRemove(Service service) {
    services.remove(service);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Server server = (Server) o;
    return Objects.equals(identity, server.identity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identity);
  }

  /**
   * Destroy the object
   */
  public void destroy() {
    services.forEach(s -> s.justRemove(this));
    identity.destroy();
  }

  /**
   * Notify the interaction with the server.
   * Reset the retry ping count and last interaction date
   */
  public void interaction() {
    lastInteract = System.currentTimeMillis();
    remainInterval = 3;
  }

  /**
   * Check if the last interaction date older than a ping interval
   *
   * @return true if a ping is necessary
   */
  public boolean notResponding() {
    return System.currentTimeMillis() - lastInteract > Constants.PING_INTERVAL;
  }

  /**
   * Decrement remain retry count of ping and check if server should ping
   *
   * @return server does not pass the retry count
   */
  public boolean retryPing() {
    return --remainInterval >= 0;
  }

}
