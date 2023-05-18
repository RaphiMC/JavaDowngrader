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
package net.raphimc.javadowngrader.transformer.j8.methodcallreplacer;

import net.raphimc.javadowngrader.transformer.MethodCallReplacer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class ListOfMCR implements MethodCallReplacer {

    @Override
    public InsnList getReplacement(ClassNode classNode, MethodNode methodNode, String originalName, String originalDesc) {
        final InsnList replacement = new InsnList();

        final Type[] args = Type.getArgumentTypes(originalDesc);
        if (args.length != 1 || args[0].getSort() != Type.ARRAY) {
            final int argCount = args.length;
            if (argCount == 0) {
                //
                replacement.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "java/util/Collections",
                    "emptyList",
                    "()Ljava/util/List;"
                ));
                // List
                return replacement;
            } else if (argCount == 1) {
                // Object
                replacement.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "java/util/Collections",
                    "singletonList",
                    "(Ljava/lang/Object;)Ljava/util/List;"
                ));
                // List
                return replacement;
            }

            // Object...
            replacement.add(new TypeInsnNode(Opcodes.NEW, "java/util/ArrayList"));
            // Object... ArrayList?
            replacement.add(new InsnNode(Opcodes.DUP));
            // Object... ArrayList? ArrayList?
            replacement.add(new IntInsnNode(Opcodes.SIPUSH, argCount));
            // Object... ArrayList? ArrayList? int
            replacement.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "(I)V"));
            // Object... ArrayList
            for (int i = 0; i < argCount; i++) {
                // Object... Object ArrayList
                replacement.add(new InsnNode(Opcodes.DUP_X1));
                // Object... ArrayList Object ArrayList
                replacement.add(new InsnNode(Opcodes.DUP_X1));
                // Object... ArrayList ArrayList Object ArrayList
                replacement.add(new InsnNode(Opcodes.POP));
                // Object... ArrayList ArrayList Object
                replacement.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "java/util/Objects",
                    "requireNonNull",
                    "(Ljava/lang/Object;)Ljava/lang/Object;"
                ));
                // Object... ArrayList ArrayList Object
                replacement.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z"));
                // Object... ArrayList boolean
                replacement.add(new InsnNode(Opcodes.POP));
                // Object... ArrayList
            }
            // ArrayList
            replacement.add(new InsnNode(Opcodes.DUP));
            // ArrayList ArrayList
            replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Collections", "reverse", "(Ljava/util/List;)V"));
            // ArrayList
        } else {
            final LabelNode forStart = new LabelNode();
            final LabelNode forEnd = new LabelNode();

            // Object[]
            replacement.add(new InsnNode(Opcodes.ICONST_0));
            // Object[] int(i)
            replacement.add(forStart);
            // Object[] int(i)
            replacement.add(new InsnNode(Opcodes.DUP_X1));
            // int(i) Object[] int(i)
            replacement.add(new InsnNode(Opcodes.SWAP));
            // int(i) int(i) Object[]
            replacement.add(new InsnNode(Opcodes.DUP_X2));
            // Object[] int(i) int(i) Object[]
            replacement.add(new InsnNode(Opcodes.ARRAYLENGTH));
            // Object[] int(i) int(i) int(length)
            replacement.add(new JumpInsnNode(Opcodes.IF_ICMPGE, forEnd));
            // Object[] int(i)
            replacement.add(new InsnNode(Opcodes.DUP2));
            // Object[] int(i) Object[] int(i)
            replacement.add(new InsnNode(Opcodes.AALOAD));
            // Object[] int(i) Object
            replacement.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "java/util/Objects",
                "requireNonNull",
                "(Ljava/lang/Object;)Ljava/lang/Object;"
            ));
            // Object[] int(i) Object
            replacement.add(new InsnNode(Opcodes.POP));
            // Object[] int(i)
            replacement.add(new InsnNode(Opcodes.ICONST_1));
            // Object[] int(i) int
            replacement.add(new InsnNode(Opcodes.IADD));
            // Object[] int(i)
            replacement.add(new JumpInsnNode(Opcodes.GOTO, forStart));

            replacement.add(forEnd);
            // Object[] int(i)
            replacement.add(new InsnNode(Opcodes.POP));
            // Object[]
            replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Arrays", "asList", "([Ljava/lang/Object;)Ljava/util/List;"));
            // List
        }
        // List
        replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Collections", "unmodifiableList", "(Ljava/util/List;)Ljava/util/List;"));
        // List

        return replacement;
    }

}
