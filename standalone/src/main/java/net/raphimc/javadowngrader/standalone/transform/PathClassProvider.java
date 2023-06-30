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
package net.raphimc.javadowngrader.standalone.transform;

import net.lenni0451.classtransform.utils.tree.IClassProvider;
import net.raphimc.javadowngrader.standalone.util.GeneralUtil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PathClassProvider extends AbstractClassProvider {

    private final Path root;

    public PathClassProvider(Path root, IClassProvider parent) {
        super(parent);
        this.root = root;
    }

    @Override
    public byte[] getClass(String name) {
        final Path path = root.resolve(GeneralUtil.toClassFilename(name));
        if (Files.exists(path)) {
            try {
                return Files.readAllBytes(path);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        return super.getClass(name);
    }

    @Override
    public Map<String, Supplier<byte[]>> getAllClasses() {
        try (Stream<Path> stream = Files.walk(root)) {
            return GeneralUtil.merge(
                (a, b) -> b,
                super.getAllClasses(),
                stream
                    .filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().endsWith(".class"))
                    .collect(Collectors.toMap(
                        p -> GeneralUtil.toClassName(GeneralUtil.slashName(root.relativize(p))),
                        p -> () -> {
                            try {
                                return Files.readAllBytes(p);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }
                    ))
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
