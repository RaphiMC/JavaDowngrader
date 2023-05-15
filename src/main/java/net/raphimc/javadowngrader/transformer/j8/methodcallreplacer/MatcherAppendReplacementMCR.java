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

public class MatcherAppendReplacementMCR implements MethodCallReplacer {

    @Override
    public InsnList getReplacement(ClassNode classNode, MethodNode methodNode, String originalDesc) {
        final InsnList replacement = new InsnList();

        final int stringBufferIndex = ASMUtil.getFreeVarIndex(methodNode);
        replacement.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuffer"));
        replacement.add(new InsnNode(Opcodes.DUP));
        replacement.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "()V"));
        replacement.add(new VarInsnNode(Opcodes.ASTORE, stringBufferIndex));

        replacement.add(new InsnNode(Opcodes.DUP2_X1));
        replacement.add(new InsnNode(Opcodes.POP2));
        replacement.add(new InsnNode(Opcodes.SWAP));
        replacement.add(new VarInsnNode(Opcodes.ALOAD, stringBufferIndex));
        replacement.add(new InsnNode(Opcodes.SWAP));
        replacement.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/regex/Matcher", "appendReplacement", "(Ljava/lang/StringBuffer;Ljava/lang/String;)Ljava/util/regex/Matcher;"));
        replacement.add(new InsnNode(Opcodes.SWAP));
        replacement.add(new VarInsnNode(Opcodes.ALOAD, stringBufferIndex));
        replacement.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/StringBuffer;)Ljava/lang/StringBuilder;"));
        replacement.add(new InsnNode(Opcodes.POP));

        return replacement;
    }

}
