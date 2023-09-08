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
import net.raphimc.javadowngrader.transformer.MethodCallReplacer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class BufferMCR implements MethodCallReplacer {

    private final String bufferClass;

    public BufferMCR(final String bufferClass) {
        this.bufferClass = bufferClass;
    }

    @Override
    public InsnList getReplacement(ClassNode classNode, MethodNode methodNode, String originalName, String originalDesc, RuntimeDepCollector depCollector) {
        final String newDesc = Type.getMethodDescriptor(Type.getObjectType("java/nio/Buffer"), Type.getArgumentTypes(originalDesc));

        final InsnList replacement = new InsnList();
        replacement.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, bufferClass, originalName, newDesc));
        replacement.add(new TypeInsnNode(Opcodes.CHECKCAST, this.bufferClass));
        return replacement;
    }

}
