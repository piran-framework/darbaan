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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Objects;

/**
 * Immutable class representing responses of the messages. Every request has a consequence
 * response and the type of the response could be determined by the status field. if status field
 * is 200, request successfully processed and the response is available on the response field
 *
 * @author Isa Hekmatizadeh
 */
public class Response {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        MAPPER.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
    }

    private final String requestId;
    private final int status;
    private final byte[] responseBytes;
    private Object response;

    Response(String requestId, int status, byte[] responseBytes) {
        this.requestId = requestId;
        this.status = status;
        this.responseBytes = responseBytes;
    }

    /**
     * Get the request id correlated to this response
     *
     * @return request id
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Get the status of the response, response code is equivalent as http status codes. Successful
     * response has 200 status code.
     *
     * @return status code of response
     */
    public int getStatus() {
        return status;
    }

    /**
     * Get response payload as pure byte array
     *
     * @return payload in byte array format
     */
    public byte[] getResponseBytes() {
        return responseBytes;
    }

    /**
     * Deserialize and return response as instance of Object class
     *
     * @return response in type of object
     * @throws IOException if deserialization failed
     */
    public Object getResponse() throws IOException {
        if (Objects.isNull(response))
            response = MAPPER.readValue(responseBytes, Object.class);
        return response;
    }

    /**
     * Deserialize and cast return response to clazz
     *
     * @param clazz type of the response
     * @return response payload in type of clazz
     * @throws IOException if deserialization failed
     */
    public <T> T getResponse(Class<T> clazz) throws IOException {
        if (Objects.isNull(response))
            response = MAPPER.readValue(responseBytes, clazz);
        return clazz.cast(response);
    }
}
