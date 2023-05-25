/*
 * This file is part of JavaDowngrader - https://github.com/RaphiMC/JavaDowngrader
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.javadowngrader.standalone.util;

import net.lenni0451.classtransform.utils.ASMUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GeneralUtil {
    public static <T> List<T> flatten(List<List<T>> list) {
        return list.stream()
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    @SafeVarargs
    public static <K, V> Map<K, V> merge(BinaryOperator<V> merger, Map<K, V>... maps) {
        return Stream.of(maps)
            .map(Map::entrySet)
            .flatMap(Set::stream)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, merger));
    }

    public static String toClassFilename(String className) {
        return ASMUtils.slash(className).concat(".class");
    }

    public static String toClassName(String classFilename) {
        return ASMUtils.dot(classFilename.substring(0, classFilename.length() - 6));
    }

    public static String slashName(Path path) {
        final String separator = path.getFileSystem().getSeparator();
        if (separator.equals("/")) {
            return path.toString();
        }
        return path.toString().replace(separator, "/");
    }

    @SuppressWarnings("DuplicateExpressions")
    public static CloseableSupplier<Path, IOException> getPath(URI uri) throws IOException {
        try {
            return CloseableSupplier.ofValue(Paths.get(uri));
        } catch (FileSystemNotFoundException e) {
            final FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
            return CloseableSupplier.ofValue(Paths.get(uri), fs::close);
        }
    }
}
