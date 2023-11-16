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
package net.raphimc.javadowngrader.transformer.j8.methodinserter;

import net.raphimc.javadowngrader.RuntimeDepCollector;
import net.raphimc.javadowngrader.transformer.DowngradeResult;
import net.raphimc.javadowngrader.transformer.MethodInserter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class PathToFileMI implements MethodInserter {

    @Override
    public void insert(ClassNode classNode, MethodNode targetMethod, RuntimeDepCollector depCollector, DowngradeResult result) {
        final InsnList code = new InsnList();

        code.add(new TypeInsnNode(Opcodes.NEW, "java/io/File"));
        // File?
        code.add(new InsnNode(Opcodes.DUP));
        // File? File?
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        // File? File? this
        code.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/nio/file/Path", "toString", "()Ljava/lang/String;"));
        // File? File? String
        code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V"));
        // File
        code.add(new InsnNode(Opcodes.ARETURN));

        targetMethod.instructions.insert(code);
    }

}
