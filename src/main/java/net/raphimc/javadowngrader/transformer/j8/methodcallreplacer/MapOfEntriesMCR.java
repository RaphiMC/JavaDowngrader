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
import net.raphimc.javadowngrader.util.ASMUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class MapOfEntriesMCR implements MethodCallReplacer {

    @Override
    public InsnList getReplacement(ClassNode classNode, MethodNode methodNode, String originalName, String originalDesc) {
        final InsnList replacement = new InsnList();

        final int mapVarIndex = ASMUtil.getFreeVarIndex(methodNode);
        final int arrayVarIndex = ASMUtil.getFreeVarIndex(methodNode) + 1;
        final int loopVarIndex = ASMUtil.getFreeVarIndex(methodNode) + 2;
        final LabelNode loopStart = new LabelNode();
        final LabelNode loopEnd = new LabelNode();

        replacement.add(new TypeInsnNode(Opcodes.NEW, "java/util/HashMap"));
        replacement.add(new InsnNode(Opcodes.DUP));
        replacement.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/HashMap", "<init>", "()V"));
        replacement.add(new VarInsnNode(Opcodes.ASTORE, mapVarIndex));

        replacement.add(new VarInsnNode(Opcodes.ASTORE, arrayVarIndex));
        replacement.add(new InsnNode(Opcodes.ICONST_0));
        replacement.add(new VarInsnNode(Opcodes.ISTORE, loopVarIndex));

        replacement.add(loopStart);
        replacement.add(new VarInsnNode(Opcodes.ALOAD, arrayVarIndex));
        replacement.add(new InsnNode(Opcodes.ARRAYLENGTH));
        replacement.add(new VarInsnNode(Opcodes.ILOAD, loopVarIndex));
        replacement.add(new JumpInsnNode(Opcodes.IF_ICMPLE, loopEnd));

        replacement.add(new VarInsnNode(Opcodes.ALOAD, mapVarIndex));
        replacement.add(new VarInsnNode(Opcodes.ALOAD, arrayVarIndex));
        replacement.add(new VarInsnNode(Opcodes.ILOAD, loopVarIndex));
        replacement.add(new InsnNode(Opcodes.AALOAD));
        replacement.add(new InsnNode(Opcodes.DUP));

        replacement.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/Map$Entry", "getKey", "()Ljava/lang/Object;"));
        replacement.add(new InsnNode(Opcodes.SWAP));
        replacement.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/Map$Entry", "getValue", "()Ljava/lang/Object;"));
        replacement.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"));
        replacement.add(new InsnNode(Opcodes.POP));

        replacement.add(new IincInsnNode(loopVarIndex, 1));
        replacement.add(new JumpInsnNode(Opcodes.GOTO, loopStart));

        replacement.add(loopEnd);
        replacement.add(new VarInsnNode(Opcodes.ALOAD, mapVarIndex));

        return replacement;
    }

}
