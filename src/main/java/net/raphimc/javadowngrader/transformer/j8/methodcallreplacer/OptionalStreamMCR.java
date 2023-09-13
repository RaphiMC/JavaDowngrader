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
package net.raphimc.javadowngrader.transformer.j8.methodcallreplacer;

import net.raphimc.javadowngrader.RuntimeDepCollector;
import net.raphimc.javadowngrader.transformer.DowngradeResult;
import net.raphimc.javadowngrader.transformer.MethodCallReplacer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class OptionalStreamMCR implements MethodCallReplacer {

    private final String streamType, optionalType, valueDescriptor;

    public OptionalStreamMCR(String baseType, String valueDescriptor) {
        this.streamType = "java/util/stream/" + baseType + "Stream";
        this.optionalType = "java/util/Optional" + baseType;
        this.valueDescriptor = valueDescriptor;
    }

    @Override
    public InsnList getReplacement(ClassNode classNode, MethodNode method, String originalName, String originalDesc, RuntimeDepCollector depCollector, DowngradeResult result) {
        final InsnList replacement = new InsnList();

        final LabelNode elseStart = new LabelNode();
        final LabelNode elseEnd = new LabelNode();

        // Optional
        replacement.add(new InsnNode(Opcodes.DUP));
        // Optional Optional
        replacement.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, optionalType, "isPresent", "()Z"));
        // Optional boolean
        replacement.add(new JumpInsnNode(Opcodes.IFEQ, elseStart));
        // Optional

        // Optional
        replacement.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, optionalType, "get", "()" + valueDescriptor));
        // Object
        replacement.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                streamType,
                "of",
                '(' + valueDescriptor + ")L" + streamType + ';',
                true
        ));
        // Stream
        replacement.add(new JumpInsnNode(Opcodes.GOTO, elseEnd));

        replacement.add(elseStart);
        // Optional
        replacement.add(new InsnNode(Opcodes.POP));
        //
        replacement.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                streamType,
                "empty",
                "()L" + streamType + ";",
                true
        ));
        // Stream

        replacement.add(elseEnd);
        // Stream

        result.setRequiresStackMapFrames();
        return replacement;
    }

}
