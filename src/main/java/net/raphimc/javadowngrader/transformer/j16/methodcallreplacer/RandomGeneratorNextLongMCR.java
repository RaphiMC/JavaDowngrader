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
package net.raphimc.javadowngrader.transformer.j16.methodcallreplacer;

import net.raphimc.javadowngrader.transformer.MethodCallReplacer;
import net.raphimc.javadowngrader.transformer.j16.RandomSupportBoundedNextLongCreator;
import net.raphimc.javadowngrader.transformer.j16.RandomSupportCheckBoundCreator;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import static net.raphimc.javadowngrader.transformer.j16.RandomSupportBoundedNextLongCreator.BOUNDEDNEXTLONG_DESC;
import static net.raphimc.javadowngrader.transformer.j16.RandomSupportBoundedNextLongCreator.BOUNDEDNEXTLONG_NAME;
import static net.raphimc.javadowngrader.transformer.j16.RandomSupportCheckBoundCreator.CHECKBOUND_DESC;
import static net.raphimc.javadowngrader.transformer.j16.RandomSupportCheckBoundCreator.CHECKBOUND_NAME;

public class RandomGeneratorNextLongMCR implements MethodCallReplacer {
    @Override
    public InsnList getReplacement(ClassNode classNode, MethodNode method, String originalName, String originalDesc) {
        final InsnList replacement = new InsnList();

        RandomSupportCheckBoundCreator.ensureHasMethod(classNode);
        RandomSupportBoundedNextLongCreator.ensureHasMethod(classNode);

        // Random long1 long2
        replacement.add(new InsnNode(Opcodes.DUP2));
        // Random long1 long2 long1 long2
        replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, classNode.name, CHECKBOUND_NAME, CHECKBOUND_DESC));
        // Random long1 long2
        replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, classNode.name, BOUNDEDNEXTLONG_NAME, BOUNDEDNEXTLONG_DESC));
        // long1 long2

        return replacement;
    }
}
