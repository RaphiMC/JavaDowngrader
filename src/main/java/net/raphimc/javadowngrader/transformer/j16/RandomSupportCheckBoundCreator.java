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
package net.raphimc.javadowngrader.transformer.j16;

import net.raphimc.javadowngrader.util.ASMUtil;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public class RandomSupportCheckBoundCreator {

    private static final String BAD_BOUND = "bound must be positive";

    public static final String CHECKBOUND_NAME = "javadowngrader-checkBound";
    public static final String CHECKBOUND_DESC = "(J)V";

    public static boolean ensureHasMethod(final ClassNode classNode) {
        if (ASMUtil.hasMethod(classNode, CHECKBOUND_NAME, CHECKBOUND_DESC)) return false;

        final MethodVisitor checkBound = classNode.visitMethod(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                CHECKBOUND_NAME, CHECKBOUND_DESC, null, null
        );
        checkBound.visitCode();

        // if (bound <= 0) {
        final Label ifEnd = new Label();
        checkBound.visitVarInsn(Opcodes.LLOAD, 0);
        checkBound.visitInsn(Opcodes.LCONST_0);
        checkBound.visitInsn(Opcodes.LCMP);
        checkBound.visitJumpInsn(Opcodes.IFGT, ifEnd);

        // throw new IllegalArgumentException(BAD_BOUND);
        checkBound.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalArgumentException");
        checkBound.visitInsn(Opcodes.DUP);
        checkBound.visitLdcInsn(BAD_BOUND);
        checkBound.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/IllegalArgumentException",
                "<init>",
                "(Ljava/lang/String;)V",
                false
        );
        checkBound.visitInsn(Opcodes.ATHROW);

        // }
        checkBound.visitLabel(ifEnd);

        // }
        checkBound.visitInsn(Opcodes.RETURN);

        checkBound.visitEnd();
        return true;
    }

}
