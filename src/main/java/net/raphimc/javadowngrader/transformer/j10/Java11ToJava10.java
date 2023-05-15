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
package net.raphimc.javadowngrader.transformer.j10;

import net.raphimc.javadowngrader.transformer.DowngradingTransformer;
import net.raphimc.javadowngrader.transformer.j10.methodcallreplacer.FilesReadStringMCR;
import net.raphimc.javadowngrader.transformer.j10.methodcallreplacer.OptionalIsEmptyMCR;
import net.raphimc.javadowngrader.transformer.j10.methodcallreplacer.StringIsBlankMCR;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class Java11ToJava10 extends DowngradingTransformer {

    public Java11ToJava10() {
        super(Opcodes.V11, Opcodes.V10);

        this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, "java/lang/String", "isBlank", "()Z", new StringIsBlankMCR());

        this.addMethodCallReplacer(Opcodes.INVOKESTATIC, "java/nio/file/Files", "readString", new FilesReadStringMCR());

        this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "isEmpty", "()Z", new OptionalIsEmptyMCR());
    }

    @Override
    protected void preTransform(ClassNode classNode) {
        this.makePackagePrivate(classNode);
    }

    private void makePackagePrivate(final ClassNode classNode) {
        if (classNode.nestHostClass == null) return;
        for (final MethodNode methodNode : classNode.methods) {
            methodNode.access &= ~Opcodes.ACC_PRIVATE;
        }
        for (final FieldNode fieldNode : classNode.fields) {
            fieldNode.access &= ~Opcodes.ACC_PRIVATE;
        }
        classNode.nestHostClass = null;
    }

}
