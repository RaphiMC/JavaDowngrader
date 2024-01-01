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
package net.raphimc.javadowngrader.transformer.j8;

import net.raphimc.javadowngrader.transformer.DowngradeResult;
import net.raphimc.javadowngrader.transformer.DowngradingTransformer;
import net.raphimc.javadowngrader.transformer.j8.methodcallreplacer.*;
import net.raphimc.javadowngrader.transformer.j8.methodinserter.PathEndsWithMI;
import net.raphimc.javadowngrader.transformer.j8.methodinserter.PathResolveMI;
import net.raphimc.javadowngrader.transformer.j8.methodinserter.PathStartsWithMI;
import net.raphimc.javadowngrader.transformer.j8.methodinserter.PathToFileMI;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class Java9ToJava8 extends DowngradingTransformer {

    public Java9ToJava8() {
        super(Opcodes.V9, Opcodes.V1_8);

        this.addMethodCallReplacer(Opcodes.INVOKESTATIC, "java/util/List", "of", new ListOfMCR());
        this.addMethodCallReplacer(Opcodes.INVOKESTATIC, "java/util/Set", "of", new SetOfMCR());
        this.addMethodCallReplacer(Opcodes.INVOKESTATIC, "java/util/Map", "of", new MapOfMCR());
        this.addMethodCallReplacer(Opcodes.INVOKESTATIC, "java/util/Map", "entry", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Map$Entry;", new MapEntryMCR());
        this.addMethodCallReplacer(Opcodes.INVOKESTATIC, "java/util/Map", "ofEntries", "([Ljava/util/Map$Entry;)Ljava/util/Map;", new MapOfEntriesMCR());

        this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, "java/io/InputStream", "transferTo", "(Ljava/io/OutputStream;)J", new InputStreamTransferToMCR());
        this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, "java/io/ByteArrayInputStream", "transferTo", "(Ljava/io/OutputStream;)J", new InputStreamTransferToMCR());
        this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, "java/io/InputStream", "readAllBytes", "()[B", new InputStreamReadAllBytesMCR());
        this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, "java/io/FileInputStream", "readAllBytes", "()[B", new InputStreamReadAllBytesMCR());
        this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, "java/io/ByteArrayInputStream", "readAllBytes", "()[B", new InputStreamReadAllBytesMCR());

        this.addMethodCallReplacer(Opcodes.INVOKESTATIC, "java/util/Objects", "requireNonNullElse", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", new ObjectsRequireNonNullElseMCR());
        this.addMethodCallReplacer(Opcodes.INVOKESTATIC, "java/util/Objects", "requireNonNullElseGet", "(Ljava/lang/Object;Ljava/util/function/Supplier;)Ljava/lang/Object;", new ObjectsRequireNonNullElseGetMCR());

        final String[] bufferClasses = new String[]{"java/nio/ByteBuffer", "java/nio/ShortBuffer", "java/nio/CharBuffer", "java/nio/IntBuffer", "java/nio/FloatBuffer", "java/nio/DoubleBuffer", "java/nio/LongBuffer"};
        for (String bufferClass : bufferClasses) {
            this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, bufferClass, "flip", "()L" + bufferClass + ';', new BufferMCR(bufferClass));
            this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, bufferClass, "clear", "()L" + bufferClass + ';', new BufferMCR(bufferClass));
            this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, bufferClass, "mark", "()L" + bufferClass + ';', new BufferMCR(bufferClass));
            this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, bufferClass, "reset", "()L" + bufferClass + ';', new BufferMCR(bufferClass));
            this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, bufferClass, "rewind", "()L" + bufferClass + ';', new BufferMCR(bufferClass));
            this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, bufferClass, "limit", "()L" + bufferClass + ';', new BufferMCR(bufferClass));
            this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, bufferClass, "limit", "(I)L" + bufferClass + ';', new BufferMCR(bufferClass));
            this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, bufferClass, "position", "(I)L" + bufferClass + ';', new BufferMCR(bufferClass));
        }

        this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "or", "(Ljava/util/function/Supplier;)Ljava/util/Optional;", new OptionalOrMCR());

        this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, "java/util/regex/Matcher", "appendReplacement", "(Ljava/lang/StringBuilder;Ljava/lang/String;)Ljava/util/regex/Matcher;", new MatcherAppendReplacementMCR());
        this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, "java/util/regex/Matcher", "appendTail", "(Ljava/lang/StringBuilder;)Ljava/lang/StringBuilder;", new MatcherAppendTailMCR());

        this.addClassReplacementWithExtraDeps("java/lang/StackWalker", "java/lang/StackWalker$Option");
        this.addClassReplacement("java/lang/StackWalker$Option");

        this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getModule", "()Ljava/lang/Module;", new ClassGetModuleMCR());
        this.addClassReplacement("java/lang/Module");
        this.addClassReplacement("java/lang/ModuleLayer");

        this.addMethodCallReplacer(Opcodes.INVOKESTATIC, "java/lang/Math", "floorMod", "(JI)I", new MathFloorModMCR());

        this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "ifPresentOrElse", new OptionalIfPresentOrElseMCR());

        this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "stream", new OptionalStreamMCR("", "Ljava/lang/Object;"));
        this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, "java/util/OptionalInt", "stream", new OptionalStreamMCR("Int", "I"));
        this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, "java/util/OptionalLong", "stream", new OptionalStreamMCR("Long", "J"));
        this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, "java/util/OptionalDouble", "stream", new OptionalStreamMCR("Double", "D"));

        this.addMethodCallReplacer(Opcodes.INVOKESTATIC, "java/lang/Runtime", "version", new RuntimeVersionMCR());
        this.addClassReplacementWithExtraDeps("java/lang/Runtime$Version", "java/lang/Runtime$VersionPattern");

        this.addMethodInserter("java/nio/file/Path", "resolve", "(Ljava/lang/String;)Ljava/nio/file/Path;", new PathResolveMI());
        this.addMethodInserter("java/nio/file/Path", "startsWith", "(Ljava/lang/String;)Z", new PathStartsWithMI());
        this.addMethodInserter("java/nio/file/Path", "endsWith", "(Ljava/lang/String;)Z", new PathEndsWithMI());
        this.addMethodInserter("java/nio/file/Path", "toFile", "()Ljava/io/File;", new PathToFileMI());
    }

    @Override
    protected void preTransform(ClassNode classNode, DowngradeResult result) {
        StringConcatFactoryReplacer.replace(classNode);
        this.makeInterfaceMethodsPublic(classNode);
    }

    private void makeInterfaceMethodsPublic(final ClassNode classNode) {
        if ((classNode.access & Opcodes.ACC_INTERFACE) != 0) {
            for (MethodNode method : classNode.methods) {
                if ((method.access & Opcodes.ACC_PRIVATE) != 0) {
                    method.access &= ~Opcodes.ACC_PRIVATE;
                }
                method.access |= Opcodes.ACC_PUBLIC;
            }
        }
    }

}
