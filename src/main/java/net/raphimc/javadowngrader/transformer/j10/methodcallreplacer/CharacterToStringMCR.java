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
package net.raphimc.javadowngrader.transformer.j10.methodcallreplacer;

import net.raphimc.javadowngrader.RuntimeDepCollector;
import net.raphimc.javadowngrader.transformer.DowngradeResult;
import net.raphimc.javadowngrader.transformer.MethodCallReplacer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class CharacterToStringMCR implements MethodCallReplacer {
    @Override
    public InsnList getReplacement(ClassNode classNode, MethodNode method, String originalName, String originalDesc, RuntimeDepCollector depCollector, DowngradeResult result) {
        final InsnList replacement = new InsnList();

        // int
        replacement.add(new TypeInsnNode(Opcodes.NEW, "java/lang/String"));
        // int String?
        replacement.add(new InsnNode(Opcodes.DUP_X1));
        // String? int String?
        replacement.add(new InsnNode(Opcodes.SWAP));
        // String? String? int
        replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Character", "toChars", "(I)[C"));
        // String? String? char[]
        replacement.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([C)V"));
        // String

        return replacement;
    }
}
