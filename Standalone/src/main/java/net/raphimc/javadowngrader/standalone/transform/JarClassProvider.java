/*
 * This file is part of JavaDowngrader - https://github.com/RaphiMC/JavaDowngrader
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.javadowngrader.standalone.transform;

import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import net.lenni0451.classtransform.utils.tree.IClassProvider;

import java.util.Map;

public class JarClassProvider extends BasicClassProvider {

    private final IClassProvider parent;
    private final Map<String, byte[]> classes;

    public JarClassProvider(final Map<String, byte[]> classes) {
        this.classes = classes;
        this.parent = null;
    }

    public JarClassProvider(final Map<String, byte[]> classes, final IClassProvider parent) {
        this.classes = classes;
        this.parent = parent;
    }

    @Override
    public byte[] getClass(String name) {
        if (this.classes.containsKey(name)) {
            return this.classes.get(name);
        } else if (this.parent != null) {
            return this.parent.getClass(name);
        }

        return super.getClass(name);
    }

}
