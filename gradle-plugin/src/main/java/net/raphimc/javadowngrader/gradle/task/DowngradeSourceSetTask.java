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
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Stream;

public abstract class DowngradeSourceSetTask extends DefaultTask {
    @Input
    public abstract Property<SourceSet> getSourceSet();

    @Input
    public abstract Property<Integer> getTargetVersion();

    @Input
    public abstract Property<Boolean> getCopyRuntimeClasses();

    public DowngradeSourceSetTask() {
        getTargetVersion().convention(Opcodes.V1_8);
        getCopyRuntimeClasses().convention(true);
    }

    @TaskAction
    public void run() throws IOException {
        for (File classesDir : getSourceSet().get().getOutput().getClassesDirs()) {
            System.out.println("Downgrading source set: " + this.getProject().getProjectDir().toPath().relativize(classesDir.toPath()));
            final Path inRoot = classesDir.toPath();

            final Collection<String> runtimeDeps = new HashSet<>();
            final TransformerManager transformerManager = new TransformerManager(
                    new PathClassProvider(inRoot, new LazyFileClassProvider(getSourceSet().get().getCompileClasspath().getFiles(), new BasicClassProvider()))
            );
            transformerManager.addBytecodeTransformer(
                    JavaDowngraderTransformer.builder(transformerManager)
                            .targetVersion(getTargetVersion().get())
                            .classFilter(c -> Files.isRegularFile(inRoot.resolve(ClassNameUtil.toClassFilename(c))))
                            .depCollector(runtimeDeps::add)
                            .build()
            );

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
            if (getCopyRuntimeClasses().get()) {
                for (final String runtimeDep : runtimeDeps) {
                    final String classPath = runtimeDep.concat(".class");
                    try (InputStream is = RuntimeRoot.class.getResourceAsStream("/" + classPath)) {
                        if (is == null) {
                            throw new IllegalStateException("Missing runtime class " + runtimeDep);
                        }
                        final Path dest = inRoot.resolve(classPath);
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
