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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GenerateJ8UnsafeSig {
    public static void main(String[] args) throws IOException {
        if (!System.getProperty("java.specification.version").equals("1.8")) {
            throw new IllegalStateException("Requires Java 8 specifically");
        }

        final ClassReader reader;
        try (InputStream is = Object.class.getResourceAsStream("/sun/misc/Unsafe.class")) {
            if (is == null) {
                throw new IllegalStateException("Missing Unsafe?");
            }
            reader = new ClassReader(is);
        }

        final ClassWriter writer = new ClassWriter(0);
        reader.accept(writer, ClassReader.SKIP_CODE);

        Files.write(Paths.get("Unsafe.sig"), writer.toByteArray());
    }
}
