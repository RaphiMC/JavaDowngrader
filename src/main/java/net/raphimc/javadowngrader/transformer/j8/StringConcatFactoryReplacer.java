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
package net.raphimc.javadowngrader.transformer.j8;

import net.raphimc.javadowngrader.util.ASMUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.Arrays;

public class StringConcatFactoryReplacer {

    private static final char STACK_ARG_CONSTANT = '\u0001';
    private static final char BSM_ARG_CONSTANT = '\u0002';

    public static void replace(final ClassNode classNode) {
        for (MethodNode methodNode : classNode.methods) {
            for (AbstractInsnNode instruction : methodNode.instructions.toArray()) {
                if (instruction.getOpcode() == Opcodes.INVOKEDYNAMIC) {
                    final InvokeDynamicInsnNode insn = (InvokeDynamicInsnNode) instruction;
                    if (insn.bsm.getOwner().equals("java/lang/invoke/StringConcatFactory") && insn.bsm.getName().equals("makeConcatWithConstants")) {
                        final String pattern = (String) insn.bsmArgs[0];
                        final Type[] stackArgs = Type.getArgumentTypes(insn.desc);
                        final Object[] bsmArgs = Arrays.copyOfRange(insn.bsmArgs, 1, insn.bsmArgs.length);
                        final int stackArgsCount = count(pattern, STACK_ARG_CONSTANT);
                        final int bsmArgsCount = count(pattern, BSM_ARG_CONSTANT);

                        if (stackArgs.length != stackArgsCount) throw new IllegalStateException("Stack args count does not match");
                        if (bsmArgs.length != bsmArgsCount) throw new IllegalStateException("BSM args count does not match");

                        int freeVarIndex = ASMUtil.getFreeVarIndex(methodNode);
                        final int[] stackIndices = new int[stackArgsCount];
                        for (int i = 0; i < stackArgs.length; i++) {
                            stackIndices[i] = freeVarIndex;
                            freeVarIndex += stackArgs[i].getSize();
                        }
                        for (int i = stackIndices.length - 1; i >= 0; i--) {
                            methodNode.instructions.insertBefore(insn, new VarInsnNode(stackArgs[i].getOpcode(Opcodes.ISTORE), stackIndices[i]));
                        }

                        final InsnList converted = convertStringConcatFactory(pattern, stackArgs, stackIndices, bsmArgs);
                        methodNode.instructions.insertBefore(insn, converted);
                        methodNode.instructions.remove(insn);
                    }
                }
            }
        }
    }

    private static InsnList convertStringConcatFactory(final String pattern, final Type[] stackArgs, final int[] stackIndices, final Object[] bsmArgs) {
        final InsnList insns = new InsnList();
        final char[] chars = pattern.toCharArray();
        int stackArgsIndex = 0;
        int bsmArgsIndex = 0;
        StringBuilder partBuilder = new StringBuilder();

        insns.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
        insns.add(new InsnNode(Opcodes.DUP));
        insns.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V"));
        for (char c : chars) {
            if (c == STACK_ARG_CONSTANT) {
                if (partBuilder.length() != 0) {
                    insns.add(new LdcInsnNode(partBuilder.toString()));
                    insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
                    partBuilder = new StringBuilder();
                }

                final Type stackArg = stackArgs[stackArgsIndex++];
                final int stackIndex = stackIndices[stackArgsIndex - 1];
                if (stackArg.getSort() == Type.OBJECT) {
                    insns.add(new VarInsnNode(Opcodes.ALOAD, stackIndex));
                    insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;"));
                } else if (stackArg.getSort() == Type.ARRAY) {
                    insns.add(new VarInsnNode(Opcodes.ALOAD, stackIndex));
                    insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Arrays", "toString", "([Ljava/lang/Object;)Ljava/lang/String;"));
                    insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
                } else {
                    insns.add(new VarInsnNode(stackArg.getOpcode(Opcodes.ILOAD), stackIndex));
                    insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(" + stackArg.getDescriptor() + ")Ljava/lang/StringBuilder;"));
                }
            } else if (c == BSM_ARG_CONSTANT) {
                insns.add(new LdcInsnNode(bsmArgs[bsmArgsIndex++]));
                insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;"));
            } else {
                partBuilder.append(c);
            }
        }
        if (partBuilder.length() != 0) {
            insns.add(new LdcInsnNode(partBuilder.toString()));
            insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
        }
        insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;"));

        return insns;
    }

    private static int count(final String s, final char search) {
        final char[] chars = s.toCharArray();
        int count = 0;
        for (char c : chars) {
            if (c == search) count++;
        }
        return count;
    }

}
