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

package com.piranframework.darbaan.exception;

import com.piranframework.darbaan.Request;

/**
 * @author Isa Hekmatizadeh
 */
public class UnknownServiceException extends RequestRelatedException {

    public UnknownServiceException(Request request) {
        super(String.format("Unknown Service Exception for service: %s-%s",
            request.getServiceName(), request.getServiceVersion()), request.getRequestId());
    }
}
