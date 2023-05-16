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
import org.objectweb.asm.tree.*;

public class ObjectsRequireNonNullElseGetMCR implements MethodCallReplacer {

    @Override
    public InsnList getReplacement(ClassNode classNode, MethodNode methodNode, String originalName, String originalDesc) {
        final InsnList replacement = new InsnList();

        final LabelNode elseJump = new LabelNode();
        final LabelNode endJump = new LabelNode();

        replacement.add(new InsnNode(Opcodes.SWAP));
        replacement.add(new InsnNode(Opcodes.DUP));
        replacement.add(new JumpInsnNode(Opcodes.IFNULL, elseJump));
        replacement.add(new InsnNode(Opcodes.SWAP));
        replacement.add(new InsnNode(Opcodes.POP));
        replacement.add(new JumpInsnNode(Opcodes.GOTO, endJump));
        replacement.add(elseJump);
        replacement.add(new InsnNode(Opcodes.POP));
        replacement.add(new LdcInsnNode("supplier"));
        replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Objects", "requireNonNull", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;"));
        replacement.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/function/Supplier", "get", "()Ljava/lang/Object;"));
        replacement.add(new LdcInsnNode("supplier.get()"));
        replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Objects", "requireNonNull", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;"));
        replacement.add(endJump);

        return replacement;
    }

}
