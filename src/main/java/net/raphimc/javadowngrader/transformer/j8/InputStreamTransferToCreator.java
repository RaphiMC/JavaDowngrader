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
package net.raphimc.javadowngrader.transformer.j8;

import net.raphimc.javadowngrader.util.ASMUtil;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public class InputStreamTransferToCreator {

    public static final String TRANSFERTO_NAME = "javadowngrader-transferTo";
    public static final String TRANSFERTO_DESC = "(Ljava/io/InputStream;Ljava/io/OutputStream;)J";

    public static void ensureHasMethod(final ClassNode classNode) {
        if (ASMUtil.hasMethod(classNode, TRANSFERTO_NAME, TRANSFERTO_DESC)) return;

        final MethodVisitor transferTo = classNode.visitMethod(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                TRANSFERTO_NAME, TRANSFERTO_DESC, null, new String[]{"java/io/IOException"}
        );
        transferTo.visitCode();

        // Objects.requireNonNull(out, "out");
        transferTo.visitVarInsn(Opcodes.ALOAD, 1);
        transferTo.visitLdcInsn("out");
        transferTo.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/util/Objects",
                "requireNonNull",
                "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;",
                false
        );
        transferTo.visitInsn(Opcodes.POP);

        // long transferred = 0;
        transferTo.visitInsn(Opcodes.LCONST_0);
        transferTo.visitVarInsn(Opcodes.LSTORE, 2);

        // byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        transferTo.visitIntInsn(Opcodes.SIPUSH, 8192);
        transferTo.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);
        transferTo.visitVarInsn(Opcodes.ASTORE, 4);

        // while ((read = this.read(buffer, 0, DEFAULT_BUFFER_SIZE)) >= 0) {
        final Label whileStart = new Label();
        final Label whileEnd = new Label();
        transferTo.visitLabel(whileStart);
        transferTo.visitVarInsn(Opcodes.ALOAD, 0);
        transferTo.visitVarInsn(Opcodes.ALOAD, 4);
        transferTo.visitInsn(Opcodes.ICONST_0);
        transferTo.visitIntInsn(Opcodes.SIPUSH, 8192);
        transferTo.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/io/InputStream",
                "read",
                "([BII)I",
                false
        );
        transferTo.visitInsn(Opcodes.DUP);
        transferTo.visitVarInsn(Opcodes.ISTORE, 5);
        transferTo.visitJumpInsn(Opcodes.IFLT, whileEnd);

        // out.write(buffer, 0, read);
        transferTo.visitVarInsn(Opcodes.ALOAD, 1);
        transferTo.visitVarInsn(Opcodes.ALOAD, 4);
        transferTo.visitInsn(Opcodes.ICONST_0);
        transferTo.visitVarInsn(Opcodes.ILOAD, 5);
        transferTo.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/io/OutputStream",
                "write",
                "([BII)V",
                false
        );

        // transferred += read;
        transferTo.visitVarInsn(Opcodes.LLOAD, 2);
        transferTo.visitVarInsn(Opcodes.ILOAD, 5);
        transferTo.visitInsn(Opcodes.I2L);
        transferTo.visitInsn(Opcodes.LADD);
        transferTo.visitVarInsn(Opcodes.LSTORE, 2);

        // }
        transferTo.visitJumpInsn(Opcodes.GOTO, whileStart);
        transferTo.visitLabel(whileEnd);

        // return transferred;
        transferTo.visitVarInsn(Opcodes.LLOAD, 2);
        transferTo.visitInsn(Opcodes.LRETURN);

        transferTo.visitEnd();
    }

}
