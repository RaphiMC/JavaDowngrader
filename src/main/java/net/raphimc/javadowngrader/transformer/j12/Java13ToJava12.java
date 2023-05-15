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
package net.raphimc.javadowngrader.transformer.j12;

import net.raphimc.javadowngrader.transformer.DowngradingTransformer;
import net.raphimc.javadowngrader.transformer.j12.methodcallreplacer.FileSystemsNewFileSystemMCR;
import org.objectweb.asm.Opcodes;

public class Java13ToJava12 extends DowngradingTransformer {

    public Java13ToJava12() {
        super(Opcodes.V13, Opcodes.V12);

        this.addMethodCallReplacer(Opcodes.INVOKESTATIC, "java/nio/file/FileSystems", "newFileSystem", "(Ljava/nio/file/Path;)Ljava/nio/file/FileSystem;", new FileSystemsNewFileSystemMCR(1));
        this.addMethodCallReplacer(Opcodes.INVOKESTATIC, "java/nio/file/FileSystems", "newFileSystem", "(Ljava/nio/file/Path;Ljava/util/Map;)Ljava/nio/file/FileSystem;", new FileSystemsNewFileSystemMCR(2));
        this.addMethodCallReplacer(Opcodes.INVOKESTATIC, "java/nio/file/FileSystems", "newFileSystem", "(Ljava/nio/file/Path;Ljava/util/Map;Ljava/lang/ClassLoader;)Ljava/nio/file/FileSystem;", new FileSystemsNewFileSystemMCR(3));
    }

}
