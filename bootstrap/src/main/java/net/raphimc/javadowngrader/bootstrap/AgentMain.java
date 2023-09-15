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
package net.raphimc.javadowngrader.bootstrap;

import net.lenni0451.classtransform.TransformerManager;
import net.raphimc.javadowngrader.bootstrap.util.FileSystemUtil;
import net.raphimc.javadowngrader.impl.classtransform.JavaDowngraderTransformer;
import net.raphimc.javadowngrader.impl.classtransform.util.ClassNameUtil;
import net.raphimc.javadowngrader.runtime.RuntimeRoot;
import net.raphimc.javadowngrader.util.Constants;
import net.raphimc.javadowngrader.util.JavaVersion;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class AgentMain {

    public static void premain(String args, Instrumentation instrumentation) throws Throwable {
        agentmain(args, instrumentation);
    }

    public static void agentmain(String args, Instrumentation instrumentation) throws Throwable {
        final Map<String, byte[]> runtimeClasses = new HashMap<>();
        try (FileSystem runtimeRootFs = FileSystemUtil.getOrCreateFileSystem(RuntimeRoot.class.getResource("").toURI())) {
            final Path runtimeRoot = runtimeRootFs.getPath('/' + Constants.JAVADOWNGRADER_RUNTIME_PACKAGE);
            try (Stream<Path> stream = Files.walk(runtimeRoot)) {
                stream.filter(Files::isRegularFile)
                        .filter(p -> !p.getFileName().toString().equals(Constants.JAVADOWNGRADER_RUNTIME_ROOT))
                        .forEach(path -> {
                            try {
                                String classFilePath = ClassNameUtil.slashName(path);
                                if (classFilePath.startsWith("/")) {
                                    classFilePath = classFilePath.substring(1);
                                }
                                runtimeClasses.put(ClassNameUtil.toClassName(classFilePath), Files.readAllBytes(path));
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
            }
        }

        instrumentation.addTransformer(new RuntimeClassesAdderTransformer(runtimeClasses));
        final TransformerManager transformerManager = new TransformerManager(new InstrumentationClassProvider(instrumentation));
        transformerManager.addBytecodeTransformer(new JavaDowngraderTransformer(transformerManager));
        transformerManager.hookInstrumentation(instrumentation);

        if (System.getProperty("spoofJavaVersion") != null) {
            final JavaVersion spoofedJavaVersion = JavaVersion.getByName(System.getProperty("spoofJavaVersion"));
            if (spoofedJavaVersion == null) {
                System.err.println("Unable to find version '" + System.getProperty("spoofJavaVersion") + "'");
                System.exit(-1);
            }
            System.setProperty("java.version", spoofedJavaVersion.getFakeJavaVersionName());
            System.setProperty("java.class.version", String.valueOf(spoofedJavaVersion.getVersion()));
            System.setProperty("java.specification.version", spoofedJavaVersion.getFakeSpecificationVersionName());
        }
    }

}
