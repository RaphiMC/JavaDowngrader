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
package net.raphimc.javadowngrader.transformer;

import net.raphimc.javadowngrader.util.Constants;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class DowngradingTransformer {

    private final int sourceVersion;
    private final int targetVersion;

    private final Map<String, MethodCallReplacer> methodCallReplacers = new HashMap<>();
    private final Map<String, String> classReplacements = new HashMap<>();

    public DowngradingTransformer(final int sourceVersion, final int targetVersion) {
        this.sourceVersion = sourceVersion;
        this.targetVersion = targetVersion;

        if (this.sourceVersion < this.targetVersion) {
            throw new IllegalArgumentException("Source version must be higher than target version");
        }
    }

    protected void addMethodCallReplacer(final int opcode, final String owner, final String name, final MethodCallReplacer replacer) {
        this.methodCallReplacers.put(owner + ';' + name, replacer);
    }

    protected void addMethodCallReplacer(final int opcode, final String owner, final String name, final String descriptor, final MethodCallReplacer replacer) {
        this.methodCallReplacers.put(owner + ';' + name + descriptor, replacer);
    }

    protected void addClassReplacement(final String name) {
        this.classReplacements.put(name, Constants.JAVADOWNGRADER_RUNTIME_PACKAGE + name);
    }

    protected void addClassReplacement(final String oldName, final String newName) {
        this.classReplacements.put(oldName, newName);
    }

    public void transform(final ClassNode classNode) {
        if (classNode.version > this.sourceVersion) {
            throw new IllegalArgumentException("Input class version is higher than supported");
        }
        if (classNode.version <= this.targetVersion) {
            return;
        }

        this.preTransform(classNode);

        int bridge = 100;

        if (!this.methodCallReplacers.isEmpty()) {
            for (MethodNode methodNode : new ArrayList<>(classNode.methods)) {
                for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
                    if (insn instanceof MethodInsnNode) {
                        final MethodInsnNode methodInsn = (MethodInsnNode) insn;

                        MethodCallReplacer replacer = this.methodCallReplacers.get(methodInsn.owner + ';' + methodInsn.name + methodInsn.desc);
                        if (replacer == null) {
                            replacer = this.methodCallReplacers.get(methodInsn.owner + ';' + methodInsn.name);
                        }
                        if (replacer != null) {
                            methodNode.instructions.insertBefore(methodInsn, replacer.getReplacement(classNode, methodNode, methodInsn.name, methodInsn.desc));
                            methodNode.instructions.remove(methodInsn);
                        }
                    } else if (insn instanceof InvokeDynamicInsnNode) {
                        final InvokeDynamicInsnNode invokeDynamicInsn = (InvokeDynamicInsnNode) insn;

                        if (invokeDynamicInsn.bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory") && invokeDynamicInsn.bsm.getName().equals("metafactory") && invokeDynamicInsn.bsm.getDesc().equals(Constants.METAFACTORY_DESC)) {
                            for (int i = 0; i < invokeDynamicInsn.bsmArgs.length; i++) {
                                final Object arg = invokeDynamicInsn.bsmArgs[i];
                                if (!(arg instanceof Handle)) continue;
                                final Handle handle = (Handle) arg;

                                MethodCallReplacer replacer = this.methodCallReplacers.get(handle.getOwner() + ';' + handle.getName() + handle.getDesc());
                                if (replacer == null) {
                                    replacer = this.methodCallReplacers.get(handle.getOwner() + ';' + handle.getName());
                                }
                                if (replacer != null) {
                                    final String desc = handle.getTag() == Opcodes.H_INVOKESTATIC || handle.getTag() == Opcodes.H_GETSTATIC || handle.getTag() == Opcodes.H_PUTSTATIC
                                        ? handle.getDesc()
                                        : "(L" + handle.getOwner() + ';' + handle.getDesc().substring(1);
                                    final MethodNode bridgeMethod = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, "javadowngrader-bridge$" + (bridge++), desc, null, null);
                                    final Type[] argumentTypes = Type.getArgumentTypes(desc);
                                    for (int i1 = 0; i1 < argumentTypes.length; i1++) {
                                        bridgeMethod.instructions.add(new VarInsnNode(argumentTypes[i1].getOpcode(Opcodes.ILOAD), i1));
                                    }
                                    bridgeMethod.instructions.add(replacer.getReplacement(classNode, bridgeMethod, handle.getName(), handle.getDesc()));
                                    bridgeMethod.instructions.add(new InsnNode(Type.getReturnType(handle.getDesc()).getOpcode(Opcodes.IRETURN)));
                                    classNode.methods.add(bridgeMethod);

                                    invokeDynamicInsn.bsmArgs[i] = new Handle(Opcodes.H_INVOKESTATIC, classNode.name, bridgeMethod.name, bridgeMethod.desc, (classNode.access & Opcodes.ACC_INTERFACE) != 0);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!this.classReplacements.isEmpty()) {
            final ClassNode remappedNode = new ClassNode();
            final ClassRemapper classRemapper = new ClassRemapper(remappedNode, new SimpleRemapper(this.classReplacements));
            classNode.accept(classRemapper);

            // Modify the class inplace
            for (Field field : ClassNode.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                if (Modifier.isFinal(field.getModifiers())) continue;
                if (!Modifier.isPublic(field.getModifiers())) continue;

                try {
                    field.set(classNode, field.get(remappedNode));
                } catch (Throwable t) {
                    throw new RuntimeException("Failed to merge class nodes", t);
                }
            }
        }

        this.postTransform(classNode);

        classNode.version = this.targetVersion;
    }

    protected void preTransform(final ClassNode classNode) {
    }

    protected void postTransform(final ClassNode classNode) {
    }

    public int getSourceVersion() {
        return this.sourceVersion;
    }

    public int getTargetVersion() {
        return this.targetVersion;
    }

}
