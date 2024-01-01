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

import joptsimple.ValueConverter;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PathConverter implements ValueConverter<List<File>> {
    @Override
    public List<File> convert(String value) {
        return Arrays.stream(value.split(File.pathSeparator))
            .map(File::new)
            .collect(Collectors.toList());
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Class<? extends List<File>> valueType() {
        return (Class) List.class;
    }

    @Override
    public String valuePattern() {
        return "paths separated by " + File.pathSeparator;
    }
}
