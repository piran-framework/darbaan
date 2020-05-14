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

/**
 * @author Isa Hekmatizadeh
 */
public class RoleHasNotPermissionException extends DarbaanException {

  public RoleHasNotPermissionException(String role,
                                       String serviceId,
                                       String actionCategory,
                                       String action) {
    super(String.format("Role %s has not permission to access service: %s/%s/%s",
        role, serviceId, actionCategory, action));
  }
}
