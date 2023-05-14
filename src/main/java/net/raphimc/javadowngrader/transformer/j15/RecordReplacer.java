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
package net.raphimc.javadowngrader.transformer.j15;

import net.raphimc.javadowngrader.util.ASMUtil;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RecordReplacer {

    private static final String EQUALS_DESC = "(Ljava/lang/Object;)Z";
    private static final String HASHCODE_DESC = "()I";
    private static final String TOSTRING_DESC = "()Ljava/lang/String;";

    private static final Map<String, String> PRIMITIVE_WRAPPERS = new HashMap<>();

    static {
        PRIMITIVE_WRAPPERS.put("V", Type.getInternalName(Void.class));
        PRIMITIVE_WRAPPERS.put("Z", Type.getInternalName(Boolean.class));
        PRIMITIVE_WRAPPERS.put("B", Type.getInternalName(Byte.class));
        PRIMITIVE_WRAPPERS.put("S", Type.getInternalName(Short.class));
        PRIMITIVE_WRAPPERS.put("C", Type.getInternalName(Character.class));
        PRIMITIVE_WRAPPERS.put("I", Type.getInternalName(Integer.class));
        PRIMITIVE_WRAPPERS.put("F", Type.getInternalName(Float.class));
        PRIMITIVE_WRAPPERS.put("J", Type.getInternalName(Long.class));
        PRIMITIVE_WRAPPERS.put("D", Type.getInternalName(Double.class));
    }

    public static void replace(final ClassNode classNode) {
        if (!classNode.superName.equals("java/lang/Record")) return;

        classNode.access &= ~Opcodes.ACC_RECORD;
        classNode.superName = "java/lang/Object";

        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.name.equals("<init>")) {
                for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
                    if (insn.getOpcode() == Opcodes.INVOKESPECIAL) {
                        MethodInsnNode min = (MethodInsnNode) insn;
                        if (min.owner.equals("java/lang/Record")) {
                            min.owner = "java/lang/Object";
                            break;
                        }
                    }
                }
            }
        }

        classNode.methods.remove(ASMUtil.getMethod(classNode, "equals", EQUALS_DESC));
        final MethodVisitor equals = classNode.visitMethod(Opcodes.ACC_PUBLIC, "equals", EQUALS_DESC, null, null);
        {
            equals.visitCode();

            equals.visitVarInsn(Opcodes.ALOAD, 0);
            equals.visitVarInsn(Opcodes.ALOAD, 1);
            final Label notSameLabel = new Label();
            equals.visitJumpInsn(Opcodes.IF_ACMPNE, notSameLabel);
            equals.visitInsn(Opcodes.ICONST_1);
            equals.visitInsn(Opcodes.IRETURN);
            equals.visitLabel(notSameLabel);

            // Original uses Class.isInstance, but I think instanceof is more fitting here
            equals.visitVarInsn(Opcodes.ALOAD, 1);
            equals.visitTypeInsn(Opcodes.INSTANCEOF, classNode.name);
            final Label notIsInstanceLabel = new Label();
            equals.visitJumpInsn(Opcodes.IFNE, notIsInstanceLabel);
            equals.visitInsn(Opcodes.ICONST_0);
            equals.visitInsn(Opcodes.IRETURN);
            equals.visitLabel(notIsInstanceLabel);

            equals.visitVarInsn(Opcodes.ALOAD, 1);
            equals.visitTypeInsn(Opcodes.CHECKCAST, classNode.name);
            equals.visitVarInsn(Opcodes.ASTORE, 2);

            final Label notEqualLabel = new Label();
            for (final RecordComponentNode component : classNode.recordComponents) {
                equals.visitVarInsn(Opcodes.ALOAD, 0);
                equals.visitFieldInsn(Opcodes.GETFIELD, classNode.name, component.name, component.descriptor);
                equals.visitVarInsn(Opcodes.ALOAD, 2);
                equals.visitFieldInsn(Opcodes.GETFIELD, classNode.name, component.name, component.descriptor);
                if (Type.getType(component.descriptor).getSort() >= Type.ARRAY) { // ARRAY or OBJECT
                    equals.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            Type.getInternalName(Objects.class),
                            "equals",
                            "(Ljava/lang/Object;Ljava/lang/Object;)Z",
                            false
                    );
                    equals.visitJumpInsn(Opcodes.IFEQ, notEqualLabel);
                    continue;
                } else if ("BSCIZ".contains(component.descriptor)) {
                    equals.visitJumpInsn(Opcodes.IF_ICMPNE, notEqualLabel);
                    continue;
                } else if (component.descriptor.equals("F")) {
                    equals.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            Type.getInternalName(Float.class),
                            "equals",
                            "(FF)Z",
                            false
                    );
                } else if (component.descriptor.equals("D")) {
                    equals.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            Type.getInternalName(Double.class),
                            "equals",
                            "(DD)Z",
                            false
                    );
                } else if (component.descriptor.equals("J")) {
                    equals.visitInsn(Opcodes.LCMP);
                } else {
                    throw new AssertionError("Unknown descriptor " + component.descriptor);
                }
                equals.visitJumpInsn(Opcodes.IFNE, notEqualLabel);
            }
            equals.visitInsn(Opcodes.ICONST_1);
            equals.visitInsn(Opcodes.IRETURN);
            equals.visitLabel(notEqualLabel);
            equals.visitInsn(Opcodes.ICONST_0);
            equals.visitInsn(Opcodes.IRETURN);

            equals.visitEnd();
        }

        classNode.methods.remove(ASMUtil.getMethod(classNode, "hashCode", HASHCODE_DESC));
        final MethodVisitor hashCode = classNode.visitMethod(Opcodes.ACC_PUBLIC, "hashCode", HASHCODE_DESC, null, null);
        {
            hashCode.visitCode();

            hashCode.visitInsn(Opcodes.ICONST_0);
            for (final RecordComponentNode component : classNode.recordComponents) {
                hashCode.visitIntInsn(Opcodes.BIPUSH, 31);
                hashCode.visitInsn(Opcodes.IMUL);
                hashCode.visitVarInsn(Opcodes.ALOAD, 0);
                hashCode.visitFieldInsn(Opcodes.GETFIELD, classNode.name, component.name, component.descriptor);
                final String owner = PRIMITIVE_WRAPPERS.get(component.descriptor);
                hashCode.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        owner != null ? owner : "java/util/Objects",
                        "hashCode",
                        "(" + (owner != null ? component.descriptor : "Ljava/lang/Object;") + ")I",
                        false
                );
                hashCode.visitInsn(Opcodes.IADD);
            }
            hashCode.visitInsn(Opcodes.IRETURN);

            hashCode.visitEnd();
        }

        classNode.methods.remove(ASMUtil.getMethod(classNode, "toString", TOSTRING_DESC));
        final MethodVisitor toString = classNode.visitMethod(Opcodes.ACC_PUBLIC, "toString", TOSTRING_DESC, null, null);
        {
            toString.visitCode();

            final StringBuilder formatString = new StringBuilder("%s[");
            for (int i = 0; i < classNode.recordComponents.size(); i++) {
                formatString.append(classNode.recordComponents.get(i).name).append("=%s");
                if (i != classNode.recordComponents.size() - 1) {
                    formatString.append(", ");
                }
            }
            formatString.append(']');

            toString.visitLdcInsn(formatString.toString());
            toString.visitIntInsn(Opcodes.SIPUSH, classNode.recordComponents.size() + 1);
            toString.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
            toString.visitInsn(Opcodes.DUP);
            toString.visitInsn(Opcodes.ICONST_0);
            toString.visitVarInsn(Opcodes.ALOAD, 0);
            toString.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Object",
                    "getClass",
                    "()Ljava/lang/Class;",
                    false
            );
            toString.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Class",
                    "getSimpleName",
                    "()Ljava/lang/String;",
                    false
            );
            toString.visitInsn(Opcodes.AASTORE);
            int i = 1;
            for (final RecordComponentNode component : classNode.recordComponents) {
                toString.visitInsn(Opcodes.DUP);
                toString.visitIntInsn(Opcodes.SIPUSH, i);
                toString.visitVarInsn(Opcodes.ALOAD, 0);
                toString.visitFieldInsn(Opcodes.GETFIELD, classNode.name, component.name, component.descriptor);
                final String owner = PRIMITIVE_WRAPPERS.get(component.descriptor);
                toString.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        owner != null ? owner : "java/util/Objects",
                        "toString",
                        "(" + (owner != null ? component.descriptor : "Ljava/lang/Object;") + ")Ljava/lang/String;",
                        false
                );
                toString.visitInsn(Opcodes.AASTORE);
                i++;
            }
            toString.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "java/lang/String",
                    "format",
                    "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;",
                    false
            );
            toString.visitInsn(Opcodes.ARETURN);

            toString.visitEnd();
        }
    }

}
