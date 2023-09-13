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
import net.raphimc.javadowngrader.transformer.j8.InputStreamTransferToCreator;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import static net.raphimc.javadowngrader.transformer.j8.InputStreamTransferToCreator.TRANSFERTO_DESC;
import static net.raphimc.javadowngrader.transformer.j8.InputStreamTransferToCreator.TRANSFERTO_NAME;

public class InputStreamReadAllBytesMCR implements MethodCallReplacer {

    @Override
    public InsnList getReplacement(ClassNode classNode, MethodNode methodNode, String originalName, String originalDesc, RuntimeDepCollector depCollector, DowngradeResult result) {
        if (InputStreamTransferToCreator.ensureHasMethod(classNode)) {
            result.setRequiresStackMapFrames();
        }

        final InsnList replacement = new InsnList();

        // InputStream
        replacement.add(new TypeInsnNode(Opcodes.NEW, "java/io/ByteArrayOutputStream"));
        // InputStream ByteArrayOutputStream?
        replacement.add(new InsnNode(Opcodes.DUP));
        // InputStream ByteArrayOutputStream? ByteArrayOutputStream?
        replacement.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/io/ByteArrayOutputStream", "<init>", "()V"));
        // InputStream ByteArrayOutputStream
        replacement.add(new InsnNode(Opcodes.DUP_X1));
        // ByteArrayOutputStream InputStream ByteArrayOutputStream
        replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, classNode.name, TRANSFERTO_NAME, TRANSFERTO_DESC));
        // ByteArrayOutputStream long1 long2
        replacement.add(new InsnNode(Opcodes.POP2));
        // ByteArrayOutputStream
        replacement.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/ByteArrayOutputStream", "toByteArray", "()[B"));
        // byte[]

        return replacement;
    }

}
