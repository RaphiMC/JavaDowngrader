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
package net.raphimc.javadowngrader.web;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.additionalclassprovider.PathClassProvider;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import net.raphimc.javadowngrader.impl.classtransform.JavaDowngraderTransformer;
import net.raphimc.javadowngrader.impl.classtransform.util.ClassNameUtil;
import net.raphimc.javadowngrader.runtime.RuntimeRoot;
import net.raphimc.javadowngrader.util.JavaVersion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class JavaDowngraderWeb {
    public static String[] supportedVersions() {
        System.out.println("test2");
        return Stream.of(JavaVersion.values()).map(JavaVersion::name).toArray(String[]::new);
    }

    public static String convert(String data, int threadCount, String targetVersion) throws Exception {
        Path inputFolder = Paths.get("/files/input");
        Path outputFolder = Paths.get("/files/output");
        try {
            Files.createDirectories(inputFolder);
            Files.createDirectories(outputFolder);
            ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(data)));
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                Path newFile = inputFolder.resolve(zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    Files.createDirectories(newFile);
                } else {
                    Files.createDirectories(newFile.getParent());
                    Files.copy(zis, newFile);
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();

            System.out.println("Opening source JAR");
            final Collection<String> runtimeDeps = Collections.newSetFromMap(new ConcurrentHashMap<>());
            final TransformerManager transformerManager = new TransformerManager(
                    new PathClassProvider(inputFolder, new BasicClassProvider())
            );
            transformerManager.addBytecodeTransformer(
                    JavaDowngraderTransformer.builder(transformerManager)
                            .targetVersion(JavaVersion.valueOf(targetVersion).getVersion())
                            .classFilter(c -> Files.isRegularFile(inputFolder.resolve(ClassNameUtil.toClassFilename(c))))
                            .depCollector(runtimeDeps::add)
                            .build()
            );

            System.out.println("Downgrading classes with " + threadCount + " thread(s)");
            final ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
            final List<Callable<Void>> tasks;
            try (Stream<Path> stream = Files.walk(inputFolder)) {
                tasks = stream.map(path -> (Callable<Void>) () -> {
                    final String relative = ClassNameUtil.slashName(inputFolder.relativize(path));
                    final Path dest = outputFolder.resolve(relative);
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(dest);
                        return null;
                    }
                    final Path parent = dest.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    if (!relative.endsWith(".class") || relative.contains("META-INF/versions/")) {
                        Files.copy(path, dest);
                        return null;
                    }
                    final String className = ClassNameUtil.toClassName(relative);
                    final byte[] bytecode = Files.readAllBytes(path);
                    byte[] result = null;
                    try {
                        result = transformerManager.transform(className, bytecode);
                    } catch (Exception e) {
                        System.err.println("Failed to transform " + className);
                        e.printStackTrace();
                    }
                    Files.write(dest, result != null ? result : bytecode);

                    return null;
                }).collect(Collectors.toList());
            }
            threadPool.invokeAll(tasks);
            threadPool.shutdown();
            if (!threadPool.awaitTermination(1, TimeUnit.MINUTES)) {
                throw new IllegalStateException("Thread pool didn't shutdown correctly");
            }

            System.out.println("Copying " + runtimeDeps.size() + " runtime class(es)");
            for (final String runtimeDep : runtimeDeps) {
                final String classPath = runtimeDep.concat(".class");
                try (InputStream is = RuntimeRoot.class.getResourceAsStream("/" + classPath)) {
                    if (is == null) {
                        System.out.println("Runtime class '" + runtimeDep + "' not found! Skipping.");
                        continue;
                    }
                    final Path dest = outputFolder.resolve(classPath);
                    final Path parent = dest.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.copy(is, dest);
                }
            }
            System.out.println("Writing final JAR");

            // Create a new zip file
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                Files.walk(outputFolder)
                        .filter(Files::isRegularFile)
                        .forEach(path -> {
                            ZipEntry zipEntry1 = new ZipEntry(inputFolder.relativize(path).toString());
                            try {
                                zos.putNextEntry(zipEntry1);
                                zos.write(Files.readAllBytes(path));
                                zos.closeEntry();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
            }

            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        } finally {
            Files.walk(inputFolder)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            Files.walk(outputFolder)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }
}
