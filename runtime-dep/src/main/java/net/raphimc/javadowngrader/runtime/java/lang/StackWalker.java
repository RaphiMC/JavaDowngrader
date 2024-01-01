/*
 * MIT License
 *
 * This file is part of JavaDowngrader - https://github.com/RaphiMC/JavaDowngrader
 * Copyright (C) 2023-2024 RK_01/RaphiMC and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.raphimc.javadowngrader.runtime.java.lang;

import java.lang.invoke.MethodHandles;
import java.util.Set;

public class StackWalker {

    private StackWalker() {
    }

    public static StackWalker getInstance() {
        return new StackWalker();
    }

    public static StackWalker getInstance(final Option option) {
        return new StackWalker();
    }

    public static StackWalker getInstance(final Set<Option> options) {
        return new StackWalker();
    }

    public static StackWalker getInstance(final Set<Option> options, final int estimatedDepth) {
        return new StackWalker();
    }

    public Class<?> getCallerClass() {
        return MethodHandles.lookup().lookupClass();
    }

    public enum Option {
        RETAIN_CLASS_REFERENCE,
    }

}
