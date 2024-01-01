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
package net.raphimc.javadowngrader.bootstrap;

import net.lenni0451.reflect.ClassLoaders;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RuntimeClassesAdderTransformer implements ClassFileTransformer {

    private final Map<String, byte[]> runtimeClasses;
    private final Set<ClassLoader> injectedLoaders = new HashSet<>();

    public RuntimeClassesAdderTransformer(final Map<String, byte[]> runtimeClasses) {
        this.runtimeClasses = runtimeClasses;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (loader != null && !this.injectedLoaders.contains(loader)) {
            this.injectedLoaders.add(loader);
            for (Map.Entry<String, byte[]> entry : this.runtimeClasses.entrySet()) {
                ClassLoaders.defineClass(loader, entry.getKey(), entry.getValue());
            }
        }

        return null;
    }

}
