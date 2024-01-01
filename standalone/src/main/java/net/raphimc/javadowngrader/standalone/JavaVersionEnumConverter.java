/*
 * This file is part of JavaDowngrader - https://github.com/RaphiMC/JavaDowngrader
 * Copyright (C) 2023-2024 RK_01/RaphiMC and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.javadowngrader.standalone;

import joptsimple.ValueConversionException;
import joptsimple.ValueConverter;
import net.raphimc.javadowngrader.util.JavaVersion;

import java.util.StringJoiner;

public class JavaVersionEnumConverter implements ValueConverter<JavaVersion> {

    @Override
    public JavaVersion convert(final String name) {
        final JavaVersion version = JavaVersion.getByName(name);
        if (version == null) {
            throw new ValueConversionException("Unable to find version '" + name + "'");
        }
        return version;
    }

    @Override
    public Class<JavaVersion> valueType() {
        return JavaVersion.class;
    }

    @Override
    public String valuePattern() {
        final StringJoiner sj = new StringJoiner(", ", "[", "]");
        for (JavaVersion version : JavaVersion.values()) {
            sj.add(version.getName());
        }
        return sj.toString();
    }

}
