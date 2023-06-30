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
package net.raphimc.javadowngrader.standalone.util;

import java.util.function.Supplier;

public interface CloseableSupplier<T, E extends Exception> extends Supplier<T>, Closeable<E> {

    static <T, E extends Exception> CloseableSupplier<T, E> of(Supplier<T> supplier, Closeable<E> closeable) {
        return new CloseableSupplier<T, E>() {
            @Override
            public void close() throws E {
                closeable.close();
            }

            @Override
            public T get() {
                return supplier.get();
            }
        };
    }

    static <T, E extends Exception> CloseableSupplier<T, E> of(Supplier<T> supplier) {
        return of(supplier, Closeable.none());
    }

    static <T, E extends Exception> CloseableSupplier<T, E> ofValue(T value, Closeable<E> closeable) {
        return of(() -> value, closeable);
    }

    static <T, E extends Exception> CloseableSupplier<T, E> ofValue(T value) {
        return ofValue(value, Closeable.none());
    }
}
