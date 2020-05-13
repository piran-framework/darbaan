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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Objects;

/**
 * Instance of this class representing a request. The process method of {@link Darbaan}
 * object accepts instance of this class.
 * <p>
 * Request id automatically generated and assigned by darbaan. Clients can use it after sending
 * request object to the process method of {@link Darbaan}
 * <p>
 * Client can set payloadBytes directly or just set the payload, on the condition of setting
 * payload Request object implicitly generate the payloadBytes from the object and may throw
 * JsonParsingException.
 *
 * @author Isa Hekmatizadeh
 */
public class Request {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        MAPPER.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
    }

    private String role;
    private String requestId;
    private String serviceName;
    private String serviceVersion;
    private String actionCategory;
    private String actionName;
    private byte[] payloadBytes = new byte[0];
    private Object payload;

    public String getRole() {
        return role;
    }

    public Request setRole(String role) {
        this.role = role;
        return this;
    }

    /**
     * Retrieve request id. request id generated just after calling process method of
     * {@link Darbaan} instance
     *
     * @return request id
     */
    public String getRequestId() {
        return requestId;
    }

    void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Request setServiceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    public String getServiceVersion() {
        return serviceVersion;
    }

    public Request setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
        return this;
    }

    public String getActionCategory() {
        return actionCategory;
    }

    public Request setActionCategory(String actionCategory) {
        this.actionCategory = actionCategory;
        return this;
    }

    public String getActionName() {
        return actionName;
    }

    public Request setActionName(String actionName) {
        this.actionName = actionName;
        return this;
    }


    public byte[] getPayloadBytes() {
        return payloadBytes;
    }

    /**
     * Set the payload bytes directly
     *
     * @param payloadBytes json serialized payload
     * @return current instance
     */
    public Request setPayloadBytes(byte[] payloadBytes) {
        this.payloadBytes = payloadBytes;
        return this;
    }

    public Object getPayload() {
        return payload;
    }

    /**
     * Set the payload object, this method automatically serialize payload
     *
     * @param payload action argument to be sent
     * @return current instance
     * @throws JsonProcessingException if serializing payload encounter exception
     */
    public Request setPayload(Object payload) throws JsonProcessingException {
        this.payload = payload;
        this.payloadBytes = MAPPER.writeValueAsBytes(payload);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Request request = (Request) o;
        return Objects.equals(requestId, request.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId);
    }
}
