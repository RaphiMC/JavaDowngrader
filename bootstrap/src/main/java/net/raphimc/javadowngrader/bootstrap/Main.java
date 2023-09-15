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
import net.lenni0451.classtransform.additionalclassprovider.LazyFileClassProvider;
import net.lenni0451.classtransform.utils.loader.InjectionClassLoader;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import net.lenni0451.reflect.ClassLoaders;
import net.lenni0451.reflect.Methods;
import net.raphimc.javadowngrader.impl.classtransform.JavaDowngraderTransformer;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.jar.JarFile;

public class Main {

    public static void main(String[] args) throws ClassNotFoundException {
        if (args.length < 1) {
            final String jarPath = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            final String jarName = jarPath.substring(jarPath.lastIndexOf('/') + 1);
            System.out.println("Usage: java -jar " + jarName + " <jar> [args]");
            return;
        }

        final File jarFile = new File(args[0]);
        if (!jarFile.isFile()) {
            System.err.println("File not found: " + jarFile.getAbsolutePath());
            return;
        }

        URL jarUrl;
        String mainClass;
        try {
            jarUrl = jarFile.toURI().toURL();

            try (final JarFile jar = new JarFile(jarFile)) {
                mainClass = jar.getManifest().getMainAttributes().getValue("Main-Class");
            }
            if (mainClass == null) throw new RuntimeException("No main class found");
        } catch (Throwable e) {
            throw new RuntimeException("Invalid jar file", e);
        }

        final URL[] systemClassPath = ClassLoaders.getSystemClassPath();
        final URL[] urls = Arrays.copyOf(systemClassPath, systemClassPath.length + 1);
        urls[urls.length - 1] = jarUrl;
        args = Arrays.copyOfRange(args, 1, args.length);

        final TransformerManager transformerManager = new TransformerManager(new LazyFileClassProvider(Collections.singletonList(jarFile), new BasicClassProvider()));
        transformerManager.addBytecodeTransformer(new JavaDowngraderTransformer(transformerManager));
        final InjectionClassLoader injectionClassLoader = new InjectionClassLoader(transformerManager, urls);
        Thread.currentThread().setContextClassLoader(injectionClassLoader);
        Methods.invoke(null, Methods.getDeclaredMethod(injectionClassLoader.loadClass(mainClass), "main", String[].class), (Object) args);
    }

}
