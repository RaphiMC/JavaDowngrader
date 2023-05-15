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
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class DowngradingTransformer {

    private final int sourceVersion;
    private final int targetVersion;

    private final Map<String, MethodCallReplacer> methodCallReplacers = new HashMap<>();

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

    public void transform(final ClassNode classNode) {
        if (classNode.version > this.sourceVersion) {
            throw new IllegalArgumentException("Input class version is higher than supported");
        }
        if (classNode.version <= this.targetVersion) {
            return;
        }

        this.preTransform(classNode);

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
                                    final MethodNode bridgeMethod = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, "javadowngrader-bridge$" + System.nanoTime(), handle.getDesc(), null, null);
                                    final Type[] argumentTypes = Type.getArgumentTypes(handle.getDesc());
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
