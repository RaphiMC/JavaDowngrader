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
package net.raphimc.javadowngrader.transformer.j9.methodcallreplacer;

import net.raphimc.javadowngrader.transformer.MethodCallReplacer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class OptionalOrElseThrowMCR implements MethodCallReplacer {

    @Override
    public InsnList getReplacement(ClassNode classNode, MethodNode method, String originalName, String originalDesc) {
        final InsnList replacement = new InsnList();

        LabelNode ifNeq = new LabelNode();
        LabelNode end = new LabelNode();

        // Optional
        replacement.add(new InsnNode(Opcodes.DUP));
        // Optional Optional
        replacement.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "isPresent", "()Z"));
        // Optional boolean
        replacement.add(new JumpInsnNode(Opcodes.IFNE, ifNeq));
        // Optional
        replacement.add(new TypeInsnNode(Opcodes.NEW, "java/util/NoSuchElementException"));
        // Optional NoSuchElementException
        replacement.add(new InsnNode(Opcodes.DUP));
        // Optional NoSuchElementException? NoSuchElementException?
        replacement.add(new LdcInsnNode("No value present"));
        // Optional NoSuchElementException? NoSuchElementException? String
        replacement.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/NoSuchElementException", "<init>", "(Ljava/lang/String;)V"));
        // Optional NoSuchElementException
        replacement.add(new InsnNode(Opcodes.ATHROW));
        //

        replacement.add(ifNeq);
        // Optional
        replacement.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "get", "()Ljava/lang/Object;"));
        // Object
        replacement.add(end);

        return replacement;
    }

}
