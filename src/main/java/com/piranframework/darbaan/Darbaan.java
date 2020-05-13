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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.piranframework.darbaan.util.IdGenerator.id;

/**
 * The starting point to connect, send message and get the result from servers.
 * Darbaan automatically discovers and connects to servers, routes the message to them and
 * routes back the responses to client.
 * An instance of this class is enough for a channel application.
 *
 * @author Isa Hekmatizadeh
 */
public final class Darbaan {

    private static final Logger log = LoggerFactory.getLogger(Darbaan.class);
    static DarbaanConfiguration configuration;
    private final Map<String, CompletableFuture<Response>> requests = new ConcurrentHashMap<>();
    private final Connector connector;
    private final ExecutorService executorService;

    private Darbaan() throws IOException {
        executorService = Executors.newFixedThreadPool(configuration.getSendThreadPoolSize());
        connector = new Connector(this::handleReceive);
    }

    /**
     * Create and return a new instance of {@link Darbaan} class.
     *
     * @param configuration configuration to use by the instance
     * @return newly created instance
     * @throws IOException if can't open network sockets
     */
    public static Darbaan newInstance(DarbaanConfiguration configuration) throws IOException {
        Darbaan.configuration = configuration;
        return new Darbaan();
    }

    /**
     * Gracefully shutdown darbaan instance
     */
    public void destroy() {
        try {
            executorService.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        executorService.shutdown();
        connector.destroy();
        log.info("Darbaan shutdown gracefully");
    }

    /**
     * Send a request and return response in async fashion.
     *
     * @param request request to be send
     * @return response a completableFuture of the actual response
     */
    public CompletableFuture<Response> process(Request request) {
        request.setRequestId(id(Constants.ID_REQ_PREFIX));
        CompletableFuture<Response> f = new CompletableFuture<>();
        requests.put(request.getRequestId(), f);
        executorService.submit(() -> handleRequest(request));
        return f;
    }

    /**
     * Check if a service available
     *
     * @param name    service name
     * @param version service version
     * @return true if and only if service available and recognised
     */
    public boolean isServiceAvailable(String name, String version) {
        return connector.isServiceAvailable(name, version);
    }

    private void handleRequest(Request request) {
        try {
            connector.send(request);
        } catch (Exception e) {
            requests.remove(request.getRequestId()).completeExceptionally(e);
        }
    }

    private void handleReceive(Response response) {
        try {
            requests.remove(response.getRequestId()).complete(response);
        } catch (Exception e) {
            log.error("Unhandled error occurred: ", e);
        }
    }
}
