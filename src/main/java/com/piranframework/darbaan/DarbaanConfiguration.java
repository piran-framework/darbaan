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

/**
 * Darbaan Configuration class. Instances of this class hold configurations of the darbaan and
 * should be send to {@link Darbaan} static constructor.
 *
 * @author Isa Hekmatizadeh
 */
public class DarbaanConfiguration {

    private final String ip;
    private final int port;
    private final int sendThreadPoolSize;
    private final int receiveThreadPoolSize;

    DarbaanConfiguration(Builder builder) {
        this.ip = builder.ip;
        this.port = builder.port;
        this.sendThreadPoolSize = builder.sendThreadPoolSize;
        this.receiveThreadPoolSize = builder.receiveThreadPoolSize;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public int getSendThreadPoolSize() {
        return sendThreadPoolSize;
    }

    public int getReceiveThreadPoolSize() {
        return receiveThreadPoolSize;
    }

    /**
     * Builder class for {@link DarbaanConfiguration}
     */
    public static class Builder {

        private String ip;
        private int port;
        private int sendThreadPoolSize = 4;
        private int receiveThreadPoolSize = 4;

        /**
         * Set the ip of the current node to use for communication to other nodes
         *
         * @param ip ip address to use
         * @return current instance
         */
        public Builder setIp(String ip) {
            this.ip = ip;
            return this;
        }

        /**
         * Set the port to use for communication to other nodes
         *
         * @param port port to use
         * @return current instance
         */
        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        /**
         * Set the number of threads to assign to outgoing messages
         *
         * @param sendThreadPoolSize outgoing thread pool size
         * @return current instance
         */
        public Builder setSendThreadPoolSize(int sendThreadPoolSize) {
            this.sendThreadPoolSize = sendThreadPoolSize;
            return this;
        }

        /**
         * Set the number of threads to assign to incoming messages
         *
         * @param receiveThreadPoolSize incoming thread pool size
         * @return current instance
         */
        public Builder setReceiveThreadPoolSize(int receiveThreadPoolSize) {
            this.receiveThreadPoolSize = receiveThreadPoolSize;
            return this;
        }

        /**
         * Build a new instance of {@link DarbaanConfiguration} and return it
         *
         * @return new instance of {@link DarbaanConfiguration}
         */
        public DarbaanConfiguration build() {
            return new DarbaanConfiguration(this);
        }
    }
}
