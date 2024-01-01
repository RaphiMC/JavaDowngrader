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
package net.raphimc.javadowngrader.transformer.j8.methodinserter;

import net.raphimc.javadowngrader.RuntimeDepCollector;
import net.raphimc.javadowngrader.transformer.DowngradeResult;
import net.raphimc.javadowngrader.transformer.MethodInserter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class PathStartsWithMI implements MethodInserter {

    @Override
    public void insert(ClassNode classNode, MethodNode targetMethod, RuntimeDepCollector depCollector, DowngradeResult result) {
        final InsnList code = new InsnList();

        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        // this
        code.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/nio/file/Path", "getFileSystem", "()Ljava/nio/file/FileSystem;"));
        // FileSystem
        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        // FileSystem String
        code.add(new InsnNode(Opcodes.ICONST_0));
        // FileSystem String int
        code.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
        // FileSystem String String[]
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/nio/file/FileSystem", "getPath", "(Ljava/lang/String;[Ljava/lang/String;)Ljava/nio/file/Path;"));
        // Path
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        // Path this
        code.add(new InsnNode(Opcodes.SWAP));
        // this Path
        code.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/nio/file/Path", "startsWith", "(Ljava/nio/file/Path;)Z"));
        // boolean
        code.add(new InsnNode(Opcodes.IRETURN));

        targetMethod.instructions.insert(code);
    }

}
