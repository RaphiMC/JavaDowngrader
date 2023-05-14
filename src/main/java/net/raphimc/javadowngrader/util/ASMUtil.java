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
package net.raphimc.javadowngrader.util;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;

public class ASMUtil {

    // https://github.com/Lenni0451/ClassTransform/blob/0590fc5c8d28181343be877fd4155caa33789eba/src/main/java/net/lenni0451/classtransform/utils/ASMUtils.java#L281
    public static int getFreeVarIndex(final MethodNode methodNode) {
        int currentIndex = 0;
        if (!Modifier.isStatic(methodNode.access)) currentIndex = 1;
        for (Type arg : Type.getArgumentTypes(methodNode.desc)) currentIndex += arg.getSize();
        for (AbstractInsnNode instruction : methodNode.instructions) {
            if ((instruction.getOpcode() >= Opcodes.ISTORE && instruction.getOpcode() <= Opcodes.ASTORE) || (instruction.getOpcode() >= Opcodes.ILOAD && instruction.getOpcode() <= Opcodes.ALOAD)) {
                VarInsnNode varInsnNode = (VarInsnNode) instruction;
                if (varInsnNode.var > currentIndex) currentIndex = varInsnNode.var;
            } else if (instruction.getOpcode() == Opcodes.IINC) {
                IincInsnNode iincInsnNode = (IincInsnNode) instruction;
                if (iincInsnNode.var > currentIndex) currentIndex = iincInsnNode.var;
            }
        }
        return currentIndex + 2;
    }

    public static boolean hasMethod(final ClassNode classNode, final String name, final String desc) {
        return getMethod(classNode, name, desc) != null;
    }

    public static MethodNode getMethod(final ClassNode classNode, final String name, final String desc) {
        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.name.equals(name) && methodNode.desc.equals(desc)) return methodNode;
        }
        return null;
    }

}
