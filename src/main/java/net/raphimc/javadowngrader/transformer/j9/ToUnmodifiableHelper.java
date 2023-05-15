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
package net.raphimc.javadowngrader.transformer.j9;

import net.raphimc.javadowngrader.util.Constants;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

public class ToUnmodifiableHelper {
    public static void toUnmodifiable(InsnList list, String type) {
        final String classType = "Ljava/util/" + type + ';';
        final String methodType = '(' + classType + ')' + classType;
        list.add(new InvokeDynamicInsnNode(
            "apply",
            "()Ljava/util/function/Function;",
            new Handle(
                Opcodes.H_INVOKESTATIC,
                "java/lang/invoke/LambdaMetafactory",
                "metafactory",
                Constants.METAFACTORY_DESC,
                false
            ),
            Type.getMethodType("(Ljava/lang/Object;)Ljava/lang/Object;"),
            new Handle(
                Opcodes.H_INVOKESTATIC,
                "java/util/Collections",
                "unmodifiable" + type,
                methodType,
                false
            ),
            Type.getMethodType(methodType)
        ));
        list.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            "java/util/stream/Collectors",
            "collectingAndThen",
            "(Ljava/util/stream/Collector;Ljava/util/function/Function;)Ljava/util/stream/Collector;"
        ));
    }
}