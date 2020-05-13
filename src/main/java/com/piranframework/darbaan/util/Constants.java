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

package com.piranframework.darbaan.util;

/**
 * @author Isa Hekmatizadeh
 */
public class Constants {

    public static final String ID_REQ_PREFIX = "RQ";
    public static final String CHANNEL_ROLE = "CHANNEL";
    public static final String SERVER_ROLE = "SERVER";
    public static final String ADMIN_ROLE = "ADMINISTRATOR";
    /**
     * Protocol Header which include protocol version
     */
    public static final String PROTOCOL_HEADER = "SADA1";
    /**
     * Protocol Commands
     * For the meaning of the commands see SAFIR-DARBAAN
     */
    public static final String INTR = "INTR";
    public static final String RINTR = "RINTR";
    public static final String PING = "PING";
    public static final String PONG = "PONG";
    public static final String REQ = "REQ";
    public static final String REP = "REP";
    public static final int PING_RETRY = 3;
    public static final long PING_INTERVAL = 5000; //millisecond

    /**
     * Dastoor Protocol constants
     */
    public static final String DST_PROTOCOL_HEADER = "DST1";
    public static final String HLT = "HLT";
    public static final String SEC_REQ = "SEC-REQ";
    public static final String PERMS = "PERMS";
    public static final long HLT_INTERVAL = 40000;
}
