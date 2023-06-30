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
package net.raphimc.javadowngrader.standalone.transform;

import net.lenni0451.classtransform.utils.tree.IClassProvider;

import java.io.File;
import java.net.URI;
import java.nio.file.FileSystems;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

public class LazyFileClassProvider extends AbstractClassProvider implements AutoCloseable {
    private final Object[] path;

    public LazyFileClassProvider(List<File> path, IClassProvider parent) {
        super(parent);
        this.path = path.toArray();
    }

    @Override
    public byte[] getClass(String name) {
        for (int i = 0; i < path.length; i++) {
            Object element = path[i];
            if (element instanceof File) {
                synchronized (path) {
                    if ((element = path[i]) instanceof File) {
                        path[i] = element = open((File)element);
                    }
                }
            }
            try {
                return ((PathClassProvider)element).getClass(name);
            } catch (NoSuchElementException ignored) {
            }
        }
        return super.getClass(name);
    }

    private static PathClassProvider open(File file) {
        try {
            return new ClosingFileSystemClassProvider(FileSystems.newFileSystem(new URI("jar:" + file.toURI()), Collections.emptyMap()), null);
        } catch (Exception e) {
            throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        for (final Object element : path) {
            if (element instanceof AutoCloseable) {
                ((AutoCloseable)element).close();
            }
        }
    }
}
