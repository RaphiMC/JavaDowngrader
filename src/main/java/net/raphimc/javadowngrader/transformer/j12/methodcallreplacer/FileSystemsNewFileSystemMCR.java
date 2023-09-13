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
package net.raphimc.javadowngrader.transformer.j12.methodcallreplacer;

import net.raphimc.javadowngrader.RuntimeDepCollector;
import net.raphimc.javadowngrader.transformer.DowngradeResult;
import net.raphimc.javadowngrader.transformer.MethodCallReplacer;
import net.raphimc.javadowngrader.transformer.j12.FileSystemsNewFileSystemCreator;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import static net.raphimc.javadowngrader.transformer.j12.FileSystemsNewFileSystemCreator.NEWFILESYSTEM_DESC;
import static net.raphimc.javadowngrader.transformer.j12.FileSystemsNewFileSystemCreator.NEWFILESYSTEM_NAME;

public class FileSystemsNewFileSystemMCR implements MethodCallReplacer {

    private final int arity;

    public FileSystemsNewFileSystemMCR(int arity) {
        this.arity = arity;
    }

    @Override
    public InsnList getReplacement(ClassNode classNode, MethodNode method, String originalName, String originalDesc, RuntimeDepCollector depCollector, DowngradeResult result) {
        if (FileSystemsNewFileSystemCreator.ensureHasMethod(classNode)) {
            result.setRequiresStackMapFrames();
        }

        final InsnList replacement = new InsnList();
        if (arity < 2) {
            replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Map", "of", "()Ljava/util/Map;"));
        }
        if (arity < 3) {
            replacement.add(new InsnNode(Opcodes.ACONST_NULL));
        }
        replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, classNode.name, NEWFILESYSTEM_NAME, NEWFILESYSTEM_DESC));
        return replacement;
    }

}
