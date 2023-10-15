/*
 * This file is part of JavaDowngrader - https://github.com/RaphiMC/JavaDowngrader
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
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
package net.raphimc.javadowngrader.coveragescanner;

import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class CtSym implements Closeable {
    private final FileSystem fs;
    private final Path root;
    private final Map<String, SortedMap<Integer, Path>> classes;

    private CtSym(Path path) throws IOException {
        fs = FileSystems.newFileSystem(path, null);
        root = fs.getRootDirectories().iterator().next();
        classes = readClasses(root);
    }

    @SuppressWarnings("resource") // Handled by iterStream
    private static Map<String, SortedMap<Integer, Path>> readClasses(Path root) throws IOException {
        final Map<String, SortedMap<Integer, Path>> result = new HashMap<>();
        IOUtil.iterStream(Files.list(root), versionSet ->
            IOUtil.iterStream(Files.list(versionSet).filter(Files::isDirectory), module -> IOUtil.iterStream(
                Files.walk(module)
                    .filter(p -> p.toString().endsWith(".sig"))
                    .filter(Files::isRegularFile),
                sigFile -> {
                    String className = module.relativize(sigFile)
                        .toString()
                        .replace(sigFile.getFileSystem().getSeparator(), ".");
                    className = className.substring(0, className.length() - 4); // ".sig".length()
                    if (className.equals("module-info") || className.endsWith("package-info")) return;
                    final SortedMap<Integer, Path> classList = result.computeIfAbsent(className, c -> new TreeMap<>());
                    versionSet.getFileName()
                        .toString()
                        .chars()
                        .map(c -> Character.digit(c, 36))
                        .forEach(version -> {
                            if (classList.put(version, sigFile) != null) {
                                throw new IllegalStateException("Duplicate sig file for version " + version + ": " + sigFile);
                            }
                        });
                }
            ))
        );
        return result;
    }

    public static CtSym open(Path path) throws IOException {
        return new CtSym(path);
    }

    @Nullable
    public SortedMap<Integer, Path> getVersions(String className) {
        return classes.get(className);
    }

    @Override
    public void close() throws IOException {
        fs.close();
    }
}
