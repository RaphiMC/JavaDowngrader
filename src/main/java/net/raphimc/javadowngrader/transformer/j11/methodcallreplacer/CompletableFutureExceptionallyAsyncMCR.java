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
package net.raphimc.javadowngrader.transformer.j11.methodcallreplacer;

import net.raphimc.javadowngrader.transformer.MethodCallReplacer;
import net.raphimc.javadowngrader.util.ASMUtil;
import net.raphimc.javadowngrader.util.Constants;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;

public class CompletableFutureExceptionallyAsyncMCR implements MethodCallReplacer {

    private static final String handleBiFunctionName = "javadowngrader-exceptionallyAsync-handleBiFunction";
    private static final String handleBiFunctionDescriptor = "(Ljava/util/concurrent/CompletableFuture;Ljava/util/function/Function;Ljava/util/concurrent/Executor;Ljava/lang/Object;Ljava/lang/Throwable;)Ljava/util/concurrent/CompletableFuture;";

    private static final String handleAsyncBiFunctionName = "javadowngrader-exceptionallyAsync-handleAsyncBiFunction";
    private static final String handleAsyncBiFunctionDescriptor = "(Ljava/util/function/Function;Ljava/lang/Object;Ljava/lang/Throwable;)Ljava/lang/Object;";

    @Override
    public InsnList getReplacement(ClassNode classNode, MethodNode method, String originalName, String originalDesc) {
        boolean isInterface = Modifier.isInterface(classNode.access);
        MethodNode handleBiFunctionBody = this.makeHandleBiFunctionBody(classNode, isInterface);
        MethodNode handleAsyncBiFunctionBody = this.makeHandleAsyncBiFunctionBody();
        classNode.methods.add(handleBiFunctionBody);
        classNode.methods.add(handleAsyncBiFunctionBody);

        int freeVarIndex = ASMUtil.getFreeVarIndex(method);
        final InsnList replacement = new InsnList();

        replacement.add(new VarInsnNode(Opcodes.ASTORE, freeVarIndex + 2));
        replacement.add(new VarInsnNode(Opcodes.ASTORE, freeVarIndex + 1));
        replacement.add(new VarInsnNode(Opcodes.ASTORE, freeVarIndex));

        replacement.add(new VarInsnNode(Opcodes.ALOAD, freeVarIndex));
        replacement.add(new InsnNode(Opcodes.DUP));
        replacement.add(new VarInsnNode(Opcodes.ALOAD, freeVarIndex + 1));
        replacement.add(new VarInsnNode(Opcodes.ALOAD, freeVarIndex + 2));

        replacement.add(new InvokeDynamicInsnNode(
                "apply",
                "(Ljava/util/concurrent/CompletableFuture;Ljava/util/function/Function;Ljava/util/concurrent/Executor;)Ljava/util/function/BiFunction;",
                new Handle(
                        Opcodes.H_INVOKESTATIC,
                        "java/lang/invoke/LambdaMetafactory",
                        "metafactory",
                        Constants.METAFACTORY_DESC,
                        false
                ),
                Type.getType("(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"),
                new Handle(
                        Opcodes.H_INVOKESTATIC,
                        classNode.name,
                        handleBiFunctionName,
                        handleBiFunctionDescriptor,
                        isInterface
                ),
                Type.getType("(Ljava/lang/Object;Ljava/lang/Throwable;)Ljava/util/concurrent/CompletableFuture;")
        ));
        replacement.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/concurrent/CompletableFuture", "handle", "(Ljava/util/function/BiFunction;)Ljava/util/concurrent/CompletableFuture;"));
        replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/function/Function", "identity", "()Ljava/util/function/Function;"));
        replacement.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/concurrent/CompletableFuture", "thenCompose", "(Ljava/util/function/Function;)Ljava/util/concurrent/CompletableFuture;"));

        return replacement;
    }

    private MethodNode makeHandleBiFunctionBody(final ClassNode classNode, final boolean isInterface) {
        LabelNode isNullJump = new LabelNode();

        MethodNode lambda = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, handleBiFunctionName, handleBiFunctionDescriptor, null, null);
        lambda.instructions.add(new VarInsnNode(Opcodes.ALOAD, 4));
        lambda.instructions.add(new JumpInsnNode(Opcodes.IFNULL, isNullJump));

        lambda.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        lambda.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        lambda.instructions.add(new InvokeDynamicInsnNode(
                "apply",
                "(Ljava/util/function/Function;)Ljava/util/function/BiFunction;",
                new Handle(
                        Opcodes.H_INVOKESTATIC,
                        "java/lang/invoke/LambdaMetafactory",
                        "metafactory",
                        Constants.METAFACTORY_DESC,
                        false
                ),
                Type.getType("(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"),
                new Handle(
                        Opcodes.H_INVOKESTATIC,
                        classNode.name,
                        handleAsyncBiFunctionName,
                        handleAsyncBiFunctionDescriptor,
                        isInterface
                ),
                Type.getType("(Ljava/lang/Object;Ljava/lang/Throwable;)Ljava/util/concurrent/CompletableFuture;")
        ));
        lambda.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        lambda.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/concurrent/CompletableFuture", "handleAsync", "(Ljava/util/function/BiFunction;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"));
        lambda.instructions.add(new InsnNode(Opcodes.ARETURN));

        lambda.instructions.add(isNullJump);
        lambda.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        lambda.instructions.add(new InsnNode(Opcodes.ARETURN));
        return lambda;
    }

    private MethodNode makeHandleAsyncBiFunctionBody() {
        MethodNode lambda = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, handleAsyncBiFunctionName, handleAsyncBiFunctionDescriptor, null, null);
        lambda.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        lambda.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        lambda.instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/function/Function", "apply", "(Ljava/lang/Object;)Ljava/lang/Object;"));
        lambda.instructions.add(new InsnNode(Opcodes.ARETURN));
        return lambda;
    }

}
