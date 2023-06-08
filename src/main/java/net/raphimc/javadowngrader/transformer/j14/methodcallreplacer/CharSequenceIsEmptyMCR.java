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
package net.raphimc.javadowngrader.transformer.j14.methodcallreplacer;

import net.raphimc.javadowngrader.transformer.MethodCallReplacer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class CharSequenceIsEmptyMCR implements MethodCallReplacer {

    @Override
    public InsnList getReplacement(ClassNode classNode, MethodNode method, String originalName, String originalDesc) {
        final InsnList replacement = new InsnList();

        LabelNode ifNeq = new LabelNode();
        LabelNode end = new LabelNode();

        // CharSequence
        replacement.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/lang/CharSequence", "length", "()I"));
        // int
        replacement.add(new InsnNode(Opcodes.ICONST_0));
        // int, int
        replacement.add(new JumpInsnNode(Opcodes.IF_ICMPNE, ifNeq));

        //
        replacement.add(new InsnNode(Opcodes.ICONST_1));
        // int
        replacement.add(new JumpInsnNode(Opcodes.GOTO, end));

        //
        replacement.add(ifNeq);
        replacement.add(new InsnNode(Opcodes.ICONST_0));
        // int
        replacement.add(end);

        return replacement;
    }

}
