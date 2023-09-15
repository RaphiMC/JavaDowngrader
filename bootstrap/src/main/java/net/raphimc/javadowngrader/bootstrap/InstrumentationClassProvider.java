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

import net.lenni0451.classtransform.utils.tree.BasicClassProvider;

import java.lang.instrument.Instrumentation;
import java.util.HashSet;
import java.util.Set;

public class InstrumentationClassProvider extends BasicClassProvider {

    private final Instrumentation instrumentation;
    private final Set<ClassLoader> classLoaders = new HashSet<>();

    public InstrumentationClassProvider(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    @Override
    public byte[] getClass(String name) throws ClassNotFoundException {
        for (int i = 0; i < 2; i++) {
            for (ClassLoader classLoader : this.classLoaders) {
                try {
                    this.classLoader = classLoader;
                    return super.getClass(name);
                } catch (ClassNotFoundException ignored) {
                }
            }
            this.refreshClassLoaders();
        }

        throw new ClassNotFoundException(name);
    }

    private void refreshClassLoaders() {
        this.classLoaders.clear();
        for (Class<?> clazz : this.instrumentation.getAllLoadedClasses()) {
            this.classLoaders.add(clazz.getClassLoader());
        }
    }

}
