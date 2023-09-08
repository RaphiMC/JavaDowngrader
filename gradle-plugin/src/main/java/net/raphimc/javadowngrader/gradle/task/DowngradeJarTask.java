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
package net.raphimc.javadowngrader.gradle.task;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import net.raphimc.javadowngrader.impl.classtransform.JavaDowngraderTransformer;
import net.raphimc.javadowngrader.impl.classtransform.classprovider.LazyFileClassProvider;
import net.raphimc.javadowngrader.impl.classtransform.classprovider.PathClassProvider;
import net.raphimc.javadowngrader.impl.classtransform.util.ClassNameUtil;
import net.raphimc.javadowngrader.runtime.RuntimeRoot;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Stream;

public class DowngradeJarTask extends DefaultTask {

    @Internal
    private File input;

    @Internal
    private String outputSuffix = "-downgraded";

    @Internal
    private FileCollection compileClassPath;

    @Internal
    private int targetVersion = Opcodes.V1_8;

    @Internal
    private boolean copyRuntimeClasses = true;

    @TaskAction
    public void run() throws IOException, URISyntaxException {
        Objects.requireNonNull(this.input, "input must be set");
        Objects.requireNonNull(this.outputSuffix, "outputSuffix must be set");
        Objects.requireNonNull(this.compileClassPath, "compileClassPath must be set");
        if (!this.input.exists()) throw new IllegalArgumentException("input does not exist");
        if (!this.input.isFile() || !this.input.getName().endsWith(".jar")) throw new IllegalArgumentException("input is not a jar file");

        System.out.println("Downgrading jar: " + this.input.getName());
        try (FileSystem inFs = FileSystems.newFileSystem(this.input.toPath(), null)) {
            final Path inRoot = inFs.getRootDirectories().iterator().next();

            final Collection<String> runtimeDeps = new HashSet<>();
            final TransformerManager transformerManager = new TransformerManager(
                    new PathClassProvider(inRoot, new LazyFileClassProvider(this.compileClassPath.getFiles(), new BasicClassProvider()))
            );
            transformerManager.addBytecodeTransformer(
                JavaDowngraderTransformer.builder(transformerManager)
                    .targetVersion(targetVersion)
                    .classFilter(c -> Files.isRegularFile(inRoot.resolve(ClassNameUtil.toClassFilename(c))))
                    .depCollector(runtimeDeps::add)
                    .build()
            );

            final String outputName = this.input.getName().substring(0, this.input.getName().length() - 4) + this.outputSuffix;
            final File outputFile = new File(this.input.getParentFile(), outputName + ".jar");

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
                                Files.copy(path, dest);
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
                if (this.copyRuntimeClasses) {
                    for (final String runtimeDep : runtimeDeps) {
                        final String classPath = runtimeDep.concat(".class");
                        try (InputStream is = RuntimeRoot.class.getResourceAsStream("/" + classPath)) {
                            if (is == null) continue;
                            final Path dest = outRoot.resolve(classPath);
                            final Path parent = dest.getParent();
                            if (parent != null) {
                                Files.createDirectories(parent);
                            }
                            Files.copy(is, dest);
                        }
                    }
                }
            }
        }
    }

    public File getInput() {
        return this.input;
    }

    public String getOutputSuffix() {
        return this.outputSuffix;
    }

    public FileCollection getCompileClassPath() {
        return this.compileClassPath;
    }

    public int getTargetVersion() {
        return this.targetVersion;
    }

    public boolean getCopyRuntimeClasses() {
        return this.copyRuntimeClasses;
    }

    public void setInput(final File input) {
        this.input = input;
    }

    public void setOutputSuffix(final String outputSuffix) {
        this.outputSuffix = outputSuffix;
    }

    public void setCompileClassPath(final FileCollection compileClassPath) {
        this.compileClassPath = compileClassPath;
    }

    public void setTargetVersion(final int targetVersion) {
        this.targetVersion = targetVersion;
    }

    public void setCopyRuntimeClasses(final boolean copyRuntimeClasses) {
        this.copyRuntimeClasses = copyRuntimeClasses;
    }

}
