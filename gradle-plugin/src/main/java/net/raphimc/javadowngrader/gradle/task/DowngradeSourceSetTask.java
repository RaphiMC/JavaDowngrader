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
import net.raphimc.javadowngrader.impl.classtransform.util.FileSystemUtil;
import net.raphimc.javadowngrader.runtime.RuntimeRoot;
import net.raphimc.javadowngrader.util.Constants;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

public class DowngradeSourceSetTask extends DefaultTask {

    @Internal
    private SourceSet sourceSet;

    @Internal
    private int targetVersion = Opcodes.V1_8;

    @Internal
    private boolean copyRuntimeClasses = true;

    @TaskAction
    public void run() throws IOException, URISyntaxException {
        Objects.requireNonNull(this.sourceSet, "sourceSet must be set");

        for (File classesDir : this.sourceSet.getOutput().getClassesDirs()) {
            System.out.println("Downgrading source set: " + this.getProject().getProjectDir().toPath().relativize(classesDir.toPath()));

            final Path inRoot = classesDir.toPath();
            final TransformerManager transformerManager = new TransformerManager(
                    new PathClassProvider(inRoot, new LazyFileClassProvider(this.sourceSet.getCompileClasspath().getFiles(), new BasicClassProvider()))
            );
            transformerManager.addBytecodeTransformer(new JavaDowngraderTransformer(
                    transformerManager, this.targetVersion, c -> Files.isRegularFile(inRoot.resolve(ClassNameUtil.toClassFilename(c)))
            ));

            // Downgrade classes
            try (Stream<Path> stream = Files.walk(inRoot)) {
                stream.forEach(path -> {
                    try {
                        final String relative = ClassNameUtil.slashName(inRoot.relativize(path));
                        if (!relative.endsWith(".class")) return;
                        final String className = ClassNameUtil.toClassName(relative);
                        final byte[] bytecode = Files.readAllBytes(path);
                        final byte[] result;
                        try {
                            result = transformerManager.transform(className, bytecode);
                        } catch (Throwable e) {
                            throw new RuntimeException("Failed to transform " + className, e);
                        }
                        if (result != null) {
                            Files.write(path, result);
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }

            // Copy runtime classes
            if (this.copyRuntimeClasses) {
                try (FileSystem runtimeRootFs = FileSystemUtil.getOrCreateFileSystem(RuntimeRoot.class.getResource("").toURI())) {
                    final Path runtimeRoot = runtimeRootFs.getPath(Constants.JAVADOWNGRADER_RUNTIME_PACKAGE);
                    try (Stream<Path> stream = Files.walk(runtimeRoot)) {
                        stream.filter(Files::isRegularFile)
                                .filter(p -> !p.getFileName().toString().equals(Constants.JAVADOWNGRADER_RUNTIME_ROOT))
                                .forEach(path -> {
                                    final String relative = ClassNameUtil.slashName(runtimeRoot.relativize(path));
                                    final Path dest = inRoot.resolve(Constants.JAVADOWNGRADER_RUNTIME_PACKAGE + relative);
                                    try {
                                        Files.createDirectories(dest.getParent());
                                        Files.copy(path, dest);
                                    } catch (IOException e) {
                                        throw new UncheckedIOException(e);
                                    }
                                });
                    }
                }
            }
        }
    }

    public SourceSet getSourceSet() {
        return this.sourceSet;
    }

    public int getTargetVersion() {
        return this.targetVersion;
    }

    public boolean getCopyRuntimeClasses() {
        return this.copyRuntimeClasses;
    }

    public void setSourceSet(final SourceSet sourceSet) {
        this.sourceSet = sourceSet;
    }

    public void setTargetVersion(final int targetVersion) {
        this.targetVersion = targetVersion;
    }

    public void setCopyRuntimeClasses(final boolean copyRuntimeClasses) {
        this.copyRuntimeClasses = copyRuntimeClasses;
    }

}
