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

package com.piranframework.darbaan.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piranframework.darbaan.Darbaan;
import com.piranframework.darbaan.DarbaanConfiguration;
import com.piranframework.darbaan.Request;
import com.piranframework.darbaan.Response;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author Isa Hekmatizadeh
 */
public class InteractiveServer {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
    Darbaan darbaan = Darbaan.newInstance(new DarbaanConfiguration.Builder()
        .setIp("192.168.13.51")
        .setPort(6001)
        .build());
    byte[] payload = new byte[0];
    try {
      payload = MAPPER.writeValueAsBytes(Collections.singletonList("salam"));
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    while (!darbaan.isServiceAvailable("test", "1"))
      Thread.sleep(10);
    System.out.println("start requesting...");

    int ch = System.in.read();
    while (ch != '0') {
      Request request = new Request()
          .setRole("USER")
          .setServiceName("test")
          .setServiceVersion("1")
          .setActionCategory("testCat")
          .setActionName("testAct")
          .setPayloadBytes(payload);
      CompletableFuture<Response> responseF = darbaan.process(request);
      responseF.handle((r, t) -> {
        if (Objects.nonNull(r))
          System.out.println("Reply: " + r);
        if (Objects.nonNull(t))
          System.out.println("Exception: " + t.getMessage());
        return "OK";
      }).get();
      ch = System.in.read();
    }
    darbaan.destroy();
  }
}
