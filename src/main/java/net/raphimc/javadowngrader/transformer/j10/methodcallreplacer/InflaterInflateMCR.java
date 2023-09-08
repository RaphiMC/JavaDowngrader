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
package net.raphimc.javadowngrader.transformer.j10.methodcallreplacer;

import net.raphimc.javadowngrader.RuntimeDepCollector;
import net.raphimc.javadowngrader.transformer.MethodCallReplacer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class InflaterInflateMCR implements MethodCallReplacer {

    @Override
    public InsnList getReplacement(ClassNode classNode, MethodNode method, String originalName, String originalDesc, RuntimeDepCollector depCollector) {
        final InsnList replacement = new InsnList();

        // Inflater ByteBuffer
        replacement.add(new InsnNode(Opcodes.DUP));
        // Inflater ByteBuffer ByteBuffer
        replacement.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "remaining", "()I"));
        // Inflater ByteBuffer int
        replacement.add(new IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_BYTE));
        // Inflater ByteBuffer byte[]
        replacement.add(new InsnNode(Opcodes.DUP2_X1));
        // ByteBuffer byte[] Inflater ByteBuffer byte[]
        replacement.add(new InsnNode(Opcodes.POP2));
        // ByteBuffer byte[] Inflater
        replacement.add(new InsnNode(Opcodes.SWAP));
        // ByteBuffer Inflater byte[]
        replacement.add(new InsnNode(Opcodes.DUP_X1));
        // ByteBuffer byte[] Inflater byte[]
        replacement.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/zip/Inflater", "inflate", "([B)I"));
        // ByteBuffer byte[] int
        replacement.add(new InsnNode(Opcodes.DUP_X2));
        // int ByteBuffer byte[] int
        replacement.add(new InsnNode(Opcodes.ICONST_0));
        // int ByteBuffer byte[] int int(0)
        replacement.add(new InsnNode(Opcodes.SWAP));
        // int ByteBuffer byte[] int(0) int
        replacement.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "put", "([BII)Ljava/nio/ByteBuffer;"));
        // int ByteBuffer
        replacement.add(new InsnNode(Opcodes.POP));
        // int

        return replacement;
    }

}
