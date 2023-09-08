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
package net.raphimc.javadowngrader.transformer.j8.methodcallreplacer;

import net.raphimc.javadowngrader.RuntimeDepCollector;
import net.raphimc.javadowngrader.transformer.MethodCallReplacer;
import net.raphimc.javadowngrader.util.ASMUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class MapOfMCR implements MethodCallReplacer {

    @Override
    public InsnList getReplacement(ClassNode classNode, MethodNode methodNode, String originalName, String originalDesc, RuntimeDepCollector depCollector) {
        final InsnList replacement = new InsnList();

        final Type[] args = Type.getArgumentTypes(originalDesc);

        final int argCount = args.length;
        if (argCount % 2 != 0) {
            throw new RuntimeException("Map.of() requires an even number of arguments");
        }

        if (argCount == 0) {
            replacement.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "java/util/Collections",
                "emptyMap",
                "()Ljava/util/Map;"
            ));
            return replacement;
        } else if (argCount == 2) {
            replacement.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "java/util/Collections",
                "singletonMap",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Map;"
            ));
            return replacement;
        }
        final int freeVarIndex = ASMUtil.getFreeVarIndex(methodNode);

        replacement.add(new TypeInsnNode(Opcodes.NEW, "java/util/HashMap"));
        replacement.add(new InsnNode(Opcodes.DUP));
        replacement.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/HashMap", "<init>", "()V"));
        replacement.add(new VarInsnNode(Opcodes.ASTORE, freeVarIndex));
        for (int i = 0; i < argCount / 2; i++) {
            replacement.add(new VarInsnNode(Opcodes.ALOAD, freeVarIndex));
            replacement.add(new InsnNode(Opcodes.DUP_X2));
            replacement.add(new InsnNode(Opcodes.POP));
            replacement.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"));
            replacement.add(new InsnNode(Opcodes.POP));
        }
        replacement.add(new VarInsnNode(Opcodes.ALOAD, freeVarIndex));
        replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Collections", "unmodifiableMap", "(Ljava/util/Map;)Ljava/util/Map;"));

        return replacement;
    }

}
