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
package net.raphimc.javadowngrader.standalone.transform;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.transformer.IBytecodeTransformer;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.raphimc.javadowngrader.JavaDowngrader;
import org.objectweb.asm.tree.ClassNode;

public class JavaDowngraderTransformer implements IBytecodeTransformer {

    private final TransformerManager transformerManager;
    private final int targetVersion;

    public JavaDowngraderTransformer(final TransformerManager transformerManager, final int targetVersion) {
        this.transformerManager = transformerManager;
        this.targetVersion = targetVersion;
    }

    @Override
    public byte[] transform(String className, byte[] bytecode, boolean calculateStackMapFrames) {
        final ClassNode classNode = ASMUtils.fromBytes(bytecode);
        if (classNode.version <= this.targetVersion) return null;

        JavaDowngrader.downgrade(classNode, this.targetVersion);

        if (calculateStackMapFrames) {
            return ASMUtils.toBytes(classNode, this.transformerManager.getClassTree(), this.transformerManager.getClassProvider());
        } else {
            return ASMUtils.toStacklessBytes(classNode);
        }
    }

}
