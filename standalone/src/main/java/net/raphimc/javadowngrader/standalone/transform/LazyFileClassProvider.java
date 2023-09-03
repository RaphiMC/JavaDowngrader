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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;

public class LazyFileClassProvider extends AbstractClassProvider implements AutoCloseable {

    private final Object[] path;

    public LazyFileClassProvider(final Collection<File> path, final IClassProvider parent) {
        super(parent);

        this.path = path.toArray();
    }

    @Override
    public byte[] getClass(String name) {
        for (int i = 0; i < this.path.length; i++) {
            Object element = this.path[i];
            if (element instanceof File) {
                synchronized (this.path) {
                    if ((element = this.path[i]) instanceof File) {
                        this.path[i] = element = open((File) element);
                    }
                }
            }
            try {
                return ((PathClassProvider) element).getClass(name);
            } catch (NoSuchElementException ignored) {
            }
        }
        return super.getClass(name);
    }

    private static PathClassProvider open(final File file) {
        final URI uri;
        try {
            uri = new URI("jar:" + file.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        try {
            return new ClosingFileSystemClassProvider(FileSystems.newFileSystem(uri, Collections.emptyMap()), null);
        } catch (FileSystemAlreadyExistsException e) {
            return new FileSystemClassProvider(FileSystems.getFileSystem(uri), null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() throws Exception {
        for (Object element : this.path) {
            if (element instanceof AutoCloseable) {
                ((AutoCloseable) element).close();
            }
        }
    }

}
