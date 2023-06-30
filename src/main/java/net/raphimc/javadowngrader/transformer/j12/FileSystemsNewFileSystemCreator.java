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
package net.raphimc.javadowngrader.transformer.j12;

import net.raphimc.javadowngrader.util.ASMUtil;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public class FileSystemsNewFileSystemCreator {
    public static final String NEWFILESYSTEM_NAME = "javadowngrader-newFileSystem";
    public static final String NEWFILESYSTEM_DESC = "(Ljava/nio/file/Path;Ljava/util/Map;Ljava/lang/ClassLoader;)Ljava/nio/file/FileSystem;";

    public static void ensureHasMethod(final ClassNode classNode) {
        if (ASMUtil.hasMethod(classNode, NEWFILESYSTEM_NAME, NEWFILESYSTEM_DESC)) return;

        final MethodVisitor newFileSystem = classNode.visitMethod(
            Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
            NEWFILESYSTEM_NAME, NEWFILESYSTEM_DESC, null, new String[] {"java/io/IOException"}
        );
        newFileSystem.visitCode();

        // if (path == null)
        newFileSystem.visitVarInsn(Opcodes.ALOAD, 0);
        final Label if1Label = new Label();
        newFileSystem.visitJumpInsn(Opcodes.IFNONNULL, if1Label);

        // throw new NullPointerException();
        newFileSystem.visitTypeInsn(Opcodes.NEW, "java/lang/NullPointerException");
        newFileSystem.visitInsn(Opcodes.DUP);
        newFileSystem.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "java/lang/NullPointerException",
            "<init>",
            "()V",
            false
        );
        newFileSystem.visitInsn(Opcodes.ATHROW);

        newFileSystem.visitLabel(if1Label);

        // for (FileSystemProvider provider: FileSystemProvider.installedProviders()) {
        newFileSystem.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "java/nio/file/spi/FileSystemProvider",
            "installedProviders",
            "()Ljava/util/List;",
            false
        );
        newFileSystem.visitMethodInsn(
            Opcodes.INVOKEINTERFACE,
            "java/util/List",
            "iterator",
            "()Ljava/util/Iterator;",
            true
        );
        newFileSystem.visitVarInsn(Opcodes.ASTORE, 3);
        final Label for1StartLabel = new Label();
        newFileSystem.visitLabel(for1StartLabel);
        newFileSystem.visitVarInsn(Opcodes.ALOAD, 3);
        newFileSystem.visitMethodInsn(
            Opcodes.INVOKEINTERFACE,
            "java/util/Iterator",
            "hasNext",
            "()Z",
            true
        );
        final Label for1EndLabel = new Label();
        newFileSystem.visitJumpInsn(Opcodes.IFEQ, for1EndLabel);
        newFileSystem.visitVarInsn(Opcodes.ALOAD, 3);
        newFileSystem.visitMethodInsn(
            Opcodes.INVOKEINTERFACE,
            "java/util/Iterator",
            "next",
            "()Ljava/lang/Object;",
            true
        );
        newFileSystem.visitTypeInsn(Opcodes.CHECKCAST, "java/nio/file/spi/FileSystemProvider");
        newFileSystem.visitVarInsn(Opcodes.ASTORE, 4);

        // try {
        final Label try1Start = new Label();
        final Label try1End = new Label();
        final Label try1Handler = new Label();
        newFileSystem.visitTryCatchBlock(try1Start, try1End, try1Handler, "java/lang/UnsupportedOperationException");
        newFileSystem.visitLabel(try1Start);

        // return provider.newFileSystem(path, env);
        newFileSystem.visitVarInsn(Opcodes.ALOAD, 4);
        newFileSystem.visitVarInsn(Opcodes.ALOAD, 0);
        newFileSystem.visitVarInsn(Opcodes.ALOAD, 1);
        newFileSystem.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/nio/file/spi/FileSystemProvider",
            "newFileSystem",
            "(Ljava/nio/file/Path;Ljava/util/Map;)Ljava/nio/file/FileSystem;",
            false
        );
        newFileSystem.visitInsn(Opcodes.ARETURN);

        // } catch (UnsupportedOperationException uoe) {
        newFileSystem.visitLabel(try1End);
        newFileSystem.visitLabel(try1Handler);

        // }
        newFileSystem.visitInsn(Opcodes.POP);

        // }
        newFileSystem.visitJumpInsn(Opcodes.GOTO, for1StartLabel);
        newFileSystem.visitLabel(for1EndLabel);

        // if (loader != null) {
        newFileSystem.visitVarInsn(Opcodes.ALOAD, 2);
        final Label if2Label = new Label();
        newFileSystem.visitJumpInsn(Opcodes.IFNULL, if2Label);

        // ServiceLoader<FileSystemProvider> sl = ServiceLoader.load(FileSystemProvider.class, loader);
        newFileSystem.visitLdcInsn(Type.getObjectType("java/nio/file/spi/FileSystemProvider"));
        newFileSystem.visitVarInsn(Opcodes.ALOAD, 2);
        newFileSystem.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "java/util/ServiceLoader",
            "load",
            "(Ljava/lang/Class;Ljava/lang/ClassLoader;)Ljava/util/ServiceLoader;",
            false
        );
        newFileSystem.visitVarInsn(Opcodes.ASTORE, 3);

        // for (FileSystemProvider provider: sl) {
        newFileSystem.visitVarInsn(Opcodes.ALOAD, 3);
        newFileSystem.visitMethodInsn(
            Opcodes.INVOKEINTERFACE,
            "java/util/ServiceLoader",
            "iterator",
            "()Ljava/util/Iterator;",
            true
        );
        newFileSystem.visitVarInsn(Opcodes.ASTORE, 4);
        final Label for2StartLabel = new Label();
        newFileSystem.visitLabel(for2StartLabel);
        newFileSystem.visitVarInsn(Opcodes.ALOAD, 4);
        newFileSystem.visitMethodInsn(
            Opcodes.INVOKEINTERFACE,
            "java/util/Iterator",
            "hasNext",
            "()Z",
            true
        );
        final Label for2EndLabel = new Label();
        newFileSystem.visitJumpInsn(Opcodes.IFEQ, for2EndLabel);
        newFileSystem.visitVarInsn(Opcodes.ALOAD, 4);
        newFileSystem.visitMethodInsn(
            Opcodes.INVOKEINTERFACE,
            "java/util/Iterator",
            "next",
            "()Ljava/lang/Object;",
            true
        );
        newFileSystem.visitTypeInsn(Opcodes.CHECKCAST, "java/nio/file/spi/FileSystemProvider");
        newFileSystem.visitVarInsn(Opcodes.ASTORE, 5);

        // try {
        final Label try2Start = new Label();
        final Label try2End = new Label();
        final Label try2Handler = new Label();
        newFileSystem.visitTryCatchBlock(try2Start, try2End, try2Handler, "java/lang/UnsupportedOperationException");
        newFileSystem.visitLabel(try2Start);

        // return provider.newFileSystem(path, env);
        newFileSystem.visitVarInsn(Opcodes.ALOAD, 5);
        newFileSystem.visitVarInsn(Opcodes.ALOAD, 0);
        newFileSystem.visitVarInsn(Opcodes.ALOAD, 1);
        newFileSystem.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/nio/file/spi/FileSystemProvider",
            "newFileSystem",
            "(Ljava/nio/file/Path;Ljava/util/Map;)Ljava/nio/file/FileSystem;",
            false
        );
        newFileSystem.visitInsn(Opcodes.ARETURN);

        // } catch (UnsupportedOperationException uoe) {
        newFileSystem.visitLabel(try2End);
        newFileSystem.visitLabel(try2Handler);

        // }
        newFileSystem.visitInsn(Opcodes.POP);

        // }
        newFileSystem.visitJumpInsn(Opcodes.GOTO, for2StartLabel);
        newFileSystem.visitLabel(for2EndLabel);

        // }
        newFileSystem.visitLabel(if2Label);

        // throw new ProviderNotFoundException("Provider not found");
        newFileSystem.visitTypeInsn(Opcodes.NEW, "java/nio/file/ProviderNotFoundException");
        newFileSystem.visitInsn(Opcodes.DUP);
        newFileSystem.visitLdcInsn("Provider not found");
        newFileSystem.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "java/nio/file/ProviderNotFoundException",
            "<init>",
            "(Ljava/lang/String;)V",
            false
        );
        newFileSystem.visitInsn(Opcodes.ATHROW);

        newFileSystem.visitEnd();
    }
}
