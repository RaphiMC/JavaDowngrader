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
package net.raphimc.javadowngrader.transformer.j16;

import net.raphimc.javadowngrader.util.ASMUtil;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public class RandomSupportBoundedNextLongCreator {
    public static final String BOUNDEDNEXTLONG_NAME = "javadowngrader-boundedNextLong";
    public static final String BOUNDEDNEXTLONG_DESC = "(Ljava/util/Random;J)J";

    public static void ensureHasMethod(final ClassNode classNode) {
        if (ASMUtil.hasMethod(classNode, BOUNDEDNEXTLONG_NAME, BOUNDEDNEXTLONG_DESC)) return;

        final MethodVisitor boundedNextLong = classNode.visitMethod(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
            BOUNDEDNEXTLONG_NAME, BOUNDEDNEXTLONG_DESC, null, null
        );
        boundedNextLong.visitCode();

        // final long m = bound - 1;
        boundedNextLong.visitVarInsn(Opcodes.LLOAD, 1);
        boundedNextLong.visitInsn(Opcodes.LCONST_1);
        boundedNextLong.visitInsn(Opcodes.LSUB);
        boundedNextLong.visitVarInsn(Opcodes.LSTORE, 3);

        // long r = rng.nextLong();
        boundedNextLong.visitVarInsn(Opcodes.ALOAD, 0);
        boundedNextLong.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/Random", "nextLong", "()J", false);
        boundedNextLong.visitVarInsn(Opcodes.LSTORE, 5);

        // if ((bound & m) == 0L) {
        final Label elseStart = new Label();
        boundedNextLong.visitVarInsn(Opcodes.LLOAD, 1);
        boundedNextLong.visitVarInsn(Opcodes.LLOAD, 3);
        boundedNextLong.visitInsn(Opcodes.LAND);
        boundedNextLong.visitInsn(Opcodes.LCONST_0);
        boundedNextLong.visitInsn(Opcodes.LCMP);
        boundedNextLong.visitJumpInsn(Opcodes.IFNE, elseStart);

        // r &= m;
        boundedNextLong.visitVarInsn(Opcodes.LLOAD, 5);
        boundedNextLong.visitVarInsn(Opcodes.LLOAD, 3);
        boundedNextLong.visitInsn(Opcodes.LAND);
        boundedNextLong.visitVarInsn(Opcodes.LSTORE, 5);

        // } else {
        final Label elseEnd = new Label();
        boundedNextLong.visitJumpInsn(Opcodes.GOTO, elseEnd);
        boundedNextLong.visitLabel(elseStart);

        // long u = r >>> 1;
        boundedNextLong.visitVarInsn(Opcodes.LLOAD, 5);
        boundedNextLong.visitInsn(Opcodes.ICONST_1);
        boundedNextLong.visitInsn(Opcodes.LUSHR);
        boundedNextLong.visitVarInsn(Opcodes.LSTORE, 7);

        // while (u + m - (r = u % bound) < 0L) {
        final Label whileStart = new Label();
        final Label whileEnd = new Label();
        boundedNextLong.visitLabel(whileStart);
        boundedNextLong.visitVarInsn(Opcodes.LLOAD, 7);
        boundedNextLong.visitVarInsn(Opcodes.LLOAD, 3);
        boundedNextLong.visitInsn(Opcodes.LADD);
        boundedNextLong.visitVarInsn(Opcodes.LLOAD, 7);
        boundedNextLong.visitVarInsn(Opcodes.LLOAD, 1);
        boundedNextLong.visitInsn(Opcodes.LREM);
        boundedNextLong.visitInsn(Opcodes.DUP2);
        boundedNextLong.visitVarInsn(Opcodes.LSTORE, 5);
        boundedNextLong.visitInsn(Opcodes.LSUB);
        boundedNextLong.visitInsn(Opcodes.LCONST_0);
        boundedNextLong.visitInsn(Opcodes.LCMP);
        boundedNextLong.visitJumpInsn(Opcodes.IFGE, whileEnd);

        // u = rng.nextLong() >>> 1;
        boundedNextLong.visitVarInsn(Opcodes.ALOAD, 0);
        boundedNextLong.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/Random", "nextLong", "()J", false);
        boundedNextLong.visitInsn(Opcodes.ICONST_1);
        boundedNextLong.visitInsn(Opcodes.LUSHR);
        boundedNextLong.visitVarInsn(Opcodes.LSTORE, 7);

        // }
        boundedNextLong.visitJumpInsn(Opcodes.GOTO, whileStart);
        boundedNextLong.visitLabel(whileEnd);

        // }
        boundedNextLong.visitLabel(elseEnd);

        // return r;
        boundedNextLong.visitVarInsn(Opcodes.LLOAD, 5);
        boundedNextLong.visitInsn(Opcodes.LRETURN);

        boundedNextLong.visitEnd();
    }
}
