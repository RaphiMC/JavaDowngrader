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
package net.raphimc.javadowngrader.transformer.j8.methodcallreplacer;

import net.raphimc.javadowngrader.RuntimeDepCollector;
import net.raphimc.javadowngrader.transformer.DowngradeResult;
import net.raphimc.javadowngrader.transformer.MethodCallReplacer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class OptionalIfPresentOrElseMCR implements MethodCallReplacer {

    @Override
    public InsnList getReplacement(ClassNode classNode, MethodNode method, String originalName, String originalDesc, RuntimeDepCollector depCollector, DowngradeResult result) {
        final InsnList replacement = new InsnList();

        final LabelNode elseStart = new LabelNode();
        final LabelNode elseEnd = new LabelNode();

        // Optional Consumer Runnable
        replacement.add(new InsnNode(Opcodes.DUP2_X1));
        // Consumer Runnable Optional Consumer Runnable
        replacement.add(new InsnNode(Opcodes.POP2));
        // Consumer Runnable Optional
        replacement.add(new InsnNode(Opcodes.DUP));
        // Consumer Runnable Optional Optional
        replacement.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "isPresent", "()Z"));
        // Consumer Runnable Optional boolean
        replacement.add(new JumpInsnNode(Opcodes.IFEQ, elseStart));
        // Consumer Runnable Optional

        // Consumer Runnable Optional
        replacement.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "get", "()Ljava/lang/Object;"));
        // Consumer Runnable Object
        replacement.add(new InsnNode(Opcodes.SWAP));
        // Consumer Object Runnable
        replacement.add(new InsnNode(Opcodes.POP));
        // Consumer Object
        replacement.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/function/Consumer", "accept", "(Ljava/lang/Object;)V"));
        //
        replacement.add(new JumpInsnNode(Opcodes.GOTO, elseEnd));

        replacement.add(elseStart);
        // Consumer Runnable Optional
        replacement.add(new InsnNode(Opcodes.POP));
        // Consumer Runnable
        replacement.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/lang/Runnable", "run", "()V"));
        // Consumer
        replacement.add(new InsnNode(Opcodes.POP));
        //

        replacement.add(elseEnd);
        //

        result.setRequiresStackMapFrames();
        return replacement;
    }

}
