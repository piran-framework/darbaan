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

import org.apache.commons.lang3.RandomStringUtils;

import java.util.StringTokenizer;

/**
 * @author Sina Bagherzadeh
 * @author Esa Hekmatizadeh
 */
public final class IdGenerator {

    private static final char[] chars = new char[]{
        'j', 's', 'o', 'e', 'f', 'a', 'u', 'm', 'g', 'c', 'x', 'q', 'z',
        'h', 'b', 'n', 't', 'p', 'k', 'd', 'y', 'v', 'l', 'i', 'w', 'r',
        '5', '9', '4', '6', '1', '7', '0', '3', '8', '2'
    };

    public static String id(String prefix) {
        if (prefix.contains("-"))
            throw new RuntimeException("prefix for id generation might not have '-'");
        String timeMillis = String.valueOf(System.currentTimeMillis());
        String random = RandomStringUtils.randomAlphanumeric(8).toUpperCase();
        return id(prefix, timeMillis, random);
    }

    public static boolean validate(String id) {
        try {
            StringTokenizer tokenizer = new StringTokenizer(id, "-");
            if (tokenizer.countTokens() != 4)
                return false;
            String prefix = tokenizer.nextToken();
            String millis = tokenizer.nextToken();
            String random = tokenizer.nextToken();
            return id(prefix, millis, random).equals(id);
        } catch (Exception e) {
            return false;
        }
    }

    private static String id(String prefix, String timeMillis, String random) {
        int sum = prefix.chars().sum() + random.chars().sum();
        StringBuilder builder = new StringBuilder();
        timeMillis.chars()
            .map(i -> (i + sum) % chars.length)
            .forEach(i -> builder.append(chars[i]));
        return String.format("%s-%s-%s-%s", prefix, timeMillis, random, builder.toString().toUpperCase());
    }
}
