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
package net.raphimc.javadowngrader.gradle.task;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.additionalclassprovider.LazyFileClassProvider;
import net.lenni0451.classtransform.additionalclassprovider.PathClassProvider;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import net.raphimc.javadowngrader.impl.classtransform.JavaDowngraderTransformer;
import net.raphimc.javadowngrader.impl.classtransform.util.ClassNameUtil;
import net.raphimc.javadowngrader.runtime.RuntimeRoot;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Stream;

public abstract class DowngradeJarTask extends DefaultTask {

    @InputFile
    public abstract RegularFileProperty getInput();

    @Input
    public abstract Property<String> getOutputSuffix();

    @InputFiles
    public abstract ConfigurableFileCollection getCompileClassPath();

    @Input
    public abstract Property<Integer> getTargetVersion();

    @Input
    public abstract Property<Boolean> getCopyRuntimeClasses();

    public DowngradeJarTask() {
        getOutputSuffix().convention("-downgraded");
        getTargetVersion().convention(Opcodes.V1_8);
        getCopyRuntimeClasses().convention(true);
    }

    @TaskAction
    public void run() throws IOException, URISyntaxException {
        final File inputFile = getInput().getAsFile().get();
        System.out.println("Downgrading jar: " + inputFile);

        try (FileSystem inFs = FileSystems.newFileSystem(inputFile.toPath(), (ClassLoader) null)) {
            final Path inRoot = inFs.getRootDirectories().iterator().next();

            final Collection<String> runtimeDeps = new HashSet<>();
            final TransformerManager transformerManager = new TransformerManager(
                    new PathClassProvider(inRoot, new LazyFileClassProvider(getCompileClassPath().getFiles(), new BasicClassProvider()))
            );
            transformerManager.addBytecodeTransformer(
                    JavaDowngraderTransformer.builder(transformerManager)
                            .targetVersion(getTargetVersion().get())
                            .classFilter(c -> Files.isRegularFile(inRoot.resolve(ClassNameUtil.toClassFilename(c))))
                            .depCollector(runtimeDeps::add)
                            .build()
            );

            final String outputName = inputFile.getName().substring(0, inputFile.getName().length() - 4) + getOutputSuffix().get();
            final File outputFile = new File(inputFile.getParentFile(), outputName + ".jar");

            try (FileSystem outFs = FileSystems.newFileSystem(new URI("jar:" + outputFile.toURI()), Collections.singletonMap("create", "true"))) {
                final Path outRoot = outFs.getRootDirectories().iterator().next();

                // Downgrade classes
                try (Stream<Path> stream = Files.walk(inRoot)) {
                    stream.forEach(path -> {
                        try {
                            final String relative = ClassNameUtil.slashName(inRoot.relativize(path));
                            final Path dest = outRoot.resolve(relative);
                            if (Files.isDirectory(path)) {
                                Files.createDirectories(dest);
                                return;
                            }
                            final Path parent = dest.getParent();
                            if (parent != null) {
                                Files.createDirectories(parent);
                            }
                            if (!relative.endsWith(".class") || relative.contains("META-INF/versions/")) {
                                Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
                                return;
                            }
                            final String className = ClassNameUtil.toClassName(relative);
                            final byte[] bytecode = Files.readAllBytes(path);
                            final byte[] result;
                            try {
                                result = transformerManager.transform(className, bytecode);
                            } catch (Throwable e) {
                                throw new RuntimeException("Failed to transform " + className, e);
                            }
                            Files.write(dest, result != null ? result : bytecode);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
                }

                // Copy runtime classes
                if (getCopyRuntimeClasses().get()) {
                    for (final String runtimeDep : runtimeDeps) {
                        final String classPath = runtimeDep.concat(".class");
                        try (InputStream is = RuntimeRoot.class.getResourceAsStream("/" + classPath)) {
                            if (is == null) {
                                throw new IllegalStateException("Missing runtime class " + runtimeDep);
                            }
                            final Path dest = outRoot.resolve(classPath);
                            final Path parent = dest.getParent();
                            if (parent != null) {
                                Files.createDirectories(parent);
                            }
                            Files.copy(is, dest, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            }
        }
    }

}
