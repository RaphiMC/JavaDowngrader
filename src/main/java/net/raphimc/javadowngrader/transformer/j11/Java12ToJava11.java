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
package net.raphimc.javadowngrader.transformer.j11;

import net.raphimc.javadowngrader.transformer.DowngradingTransformer;
import net.raphimc.javadowngrader.transformer.j11.methodcallreplacer.ClassArrayTypeMCR;
import net.raphimc.javadowngrader.transformer.j11.methodcallreplacer.CompletableFutureExceptionallyAsyncMCR;
import org.objectweb.asm.Opcodes;

public class Java12ToJava11 extends DowngradingTransformer {

    public Java12ToJava11() {
        super(Opcodes.V12, Opcodes.V11);

        this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, "java/util/concurrent/CompletableFuture", "exceptionallyAsync", "(Ljava/util/function/Function;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;", new CompletableFutureExceptionallyAsyncMCR());

        this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "arrayType", "()Ljava/lang/Class;", new ClassArrayTypeMCR());
    }

}
