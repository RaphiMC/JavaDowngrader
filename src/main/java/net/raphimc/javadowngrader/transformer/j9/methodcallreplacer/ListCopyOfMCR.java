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
package net.raphimc.javadowngrader.transformer.j9.methodcallreplacer;

import net.raphimc.javadowngrader.RuntimeDepCollector;
import net.raphimc.javadowngrader.transformer.DowngradeResult;
import net.raphimc.javadowngrader.transformer.MethodCallReplacer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class ListCopyOfMCR implements MethodCallReplacer {

    @Override
    public InsnList getReplacement(ClassNode classNode, MethodNode methodNode, String originalName, String originalDesc, RuntimeDepCollector depCollector, DowngradeResult result) {
        final InsnList replacement = new InsnList();

        // Collection
        replacement.add(new TypeInsnNode(Opcodes.NEW, "java/util/ArrayList"));
        // Collection ArrayList?
        replacement.add(new InsnNode(Opcodes.DUP_X1));
        // ArrayList? Collection ArrayList?
        replacement.add(new InsnNode(Opcodes.SWAP));
        // ArrayList? ArrayList? Collection
        replacement.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "(Ljava/util/Collection;)V"));
        // ArrayList
        replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Collections", "unmodifiableList", "(Ljava/util/List;)Ljava/util/List;"));
        // List

        return replacement;
    }

}
