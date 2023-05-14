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
package net.raphimc.javadowngrader.transformer.j10.methodcallreplacer;

import net.raphimc.javadowngrader.transformer.MethodCallReplacer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class FilesReadStringMCR implements MethodCallReplacer {

    @Override
    public InsnList getReplacement(ClassNode classNode, MethodNode methodNode, MethodInsnNode methodInsn) {
        final InsnList replacement = new InsnList();

        if (methodInsn.desc.equals("(Ljava/nio/file/Path;)Ljava/lang/String;")) {
            replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/nio/file/Files", "readAllBytes", "(Ljava/nio/file/Path;)[B"));
            replacement.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/nio/charset/StandardCharsets", "UTF_8", "Ljava/nio/charset/Charset;"));
        } else if (methodInsn.desc.equals("(Ljava/nio/file/Path;Ljava/nio/charset/Charset;)Ljava/lang/String;")) {
            replacement.add(new InsnNode(Opcodes.SWAP));
            replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/nio/file/Files", "readAllBytes", "(Ljava/nio/file/Path;)[B"));
            replacement.add(new InsnNode(Opcodes.SWAP));
        } else {
            throw new RuntimeException("Unsupported method descriptor: " + methodInsn.desc);
        }

        replacement.add(new TypeInsnNode(Opcodes.NEW, "java/lang/String"));
        replacement.add(new InsnNode(Opcodes.DUP_X2));
        replacement.add(new InsnNode(Opcodes.DUP_X2));
        replacement.add(new InsnNode(Opcodes.POP));
        replacement.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([BLjava/nio/charset/Charset;)V"));

        return replacement;
    }

}
