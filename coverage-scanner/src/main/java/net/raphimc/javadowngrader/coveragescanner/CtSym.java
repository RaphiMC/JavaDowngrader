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
package net.raphimc.javadowngrader.coveragescanner;

import net.raphimc.javadowngrader.coveragescanner.io.IOSupplier;
import net.raphimc.javadowngrader.coveragescanner.io.IOUtil;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class CtSym implements Closeable {
    private final FileSystem fs;
    private final Map<String, SortedMap<Integer, IOSupplier<InputStream>>> classes;

    private CtSym(Path path, boolean injectExtras) throws IOException {
        fs = FileSystems.newFileSystem(path, null);
        classes = readClasses(fs.getRootDirectories().iterator().next());

        if (injectExtras) {
            final URL unsafeUrl = CtSym.class.getResource("Unsafe.sig");
            if (unsafeUrl != null) {
                classes.computeIfAbsent("sun.misc.Unsafe", c -> new TreeMap<>())
                    .put(8, unsafeUrl::openStream);
            }
            readJrtClasses(classes);
        }
    }

    @SuppressWarnings("resource") // Handled by iterStream
    private static Map<String, SortedMap<Integer, IOSupplier<InputStream>>> readClasses(Path root) throws IOException {
        final Map<String, SortedMap<Integer, IOSupplier<InputStream>>> result = new HashMap<>();
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
                    result.computeIfAbsent(className, c -> new TreeMap<>()).put(
                        Character.digit(versionSet.getFileName().toString().charAt(0), 36),
                        () -> Files.newInputStream(sigFile)
                    );
                }
            ))
        );
        return result;
    }

    @SuppressWarnings("resource")
    private static void readJrtClasses(Map<String, SortedMap<Integer, IOSupplier<InputStream>>> result) throws IOException {
        final Path jrtRoot;
        try {
            jrtRoot = Paths.get(new URI("jrt:/"));
        } catch (Exception e) {
            return;
        }
        final Integer runtimeVersion = Integer.getInteger("java.specification.version");
        if (runtimeVersion == null) {
            throw new IllegalStateException("No java.specification.version");
        }
        IOUtil.iterStream(Files.list(jrtRoot), module -> IOUtil.iterStream(
            Files.walk(module)
                .filter(p -> p.toString().endsWith(".class"))
                .filter(Files::isRegularFile),
            classFile -> {
                String className = module.relativize(classFile)
                    .toString()
                    .replace(classFile.getFileSystem().getSeparator(), ".");
                className = className.substring(0, className.length() - 6); // ".class".length()
                if (className.equals("module-info") || className.endsWith("package-info")) return;
                if (className.startsWith("sun.")) {
                    // Most sun. classes aren't stored in ct.sym, making it appear like these classes were added in this version of Java.
                    return;
                }
                result.computeIfAbsent(className, c -> new TreeMap<>())
                    .put(runtimeVersion, () -> Files.newInputStream(classFile));
            }
        ));
    }

    public static CtSym open(Path path, boolean injectExtras) throws IOException {
        return new CtSym(path, injectExtras);
    }

    public static CtSym open(Path path) throws IOException {
        return new CtSym(path, true);
    }

    @Nullable
    public SortedMap<Integer, IOSupplier<InputStream>> getVersions(String className) {
        return classes.get(className);
    }

    @Override
    public void close() throws IOException {
        fs.close();
    }
}
