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
package net.raphimc.javadowngrader.transformer.j15;

import net.raphimc.javadowngrader.transformer.DowngradeResult;
import net.raphimc.javadowngrader.transformer.DowngradingTransformer;
import net.raphimc.javadowngrader.transformer.j15.methodcallreplacer.StreamToListMCR;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public class Java16ToJava15 extends DowngradingTransformer {

    public Java16ToJava15() {
        super(Opcodes.V16, Opcodes.V15);

        this.addMethodCallReplacer(Opcodes.INVOKEINTERFACE, "java/util/stream/Stream", "toList", "()Ljava/util/List;", new StreamToListMCR());

        this.addClassReplacement("java/lang/Record", ClassReplacement.ofRenameOnly("java/lang/Object"));
    }

    @Override
    protected void preTransform(ClassNode classNode, DowngradeResult result) {
        if (RecordReplacer.replace(classNode)) {
            result.setRequiresStackMapFrames();
        }
    }

}
