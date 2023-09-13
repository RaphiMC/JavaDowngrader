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
package net.raphimc.javadowngrader.impl.classtransform;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.transformer.IBytecodeTransformer;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.raphimc.javadowngrader.JavaDowngrader;
import net.raphimc.javadowngrader.RuntimeDepCollector;
import org.objectweb.asm.tree.ClassNode;

import java.util.function.Predicate;

public class JavaDowngraderTransformer implements IBytecodeTransformer {

    public static final int NATIVE_CLASS_VERSION;

    static {
        final String classVersion = System.getProperty("java.class.version");
        final String[] versions = classVersion.split("\\.");
        final int majorVersion = Integer.parseInt(versions[0]);
        final int minorVersion = Integer.parseInt(versions[1]);
        NATIVE_CLASS_VERSION = minorVersion << 16 | majorVersion;
    }

    private final TransformerManager transformerManager;
    private final int targetVersion;
    private final Predicate<String> classFilter;
    private final RuntimeDepCollector depCollector;

    public JavaDowngraderTransformer(final TransformerManager transformerManager) {
        this(transformerManager, NATIVE_CLASS_VERSION);
    }

    public JavaDowngraderTransformer(final TransformerManager transformerManager, final int targetVersion) {
        this(transformerManager, targetVersion, s -> true);
    }

    @Deprecated
    public JavaDowngraderTransformer(final TransformerManager transformerManager, final Predicate<String> classFilter) {
        this(transformerManager, NATIVE_CLASS_VERSION, classFilter);
    }

    @Deprecated
    public JavaDowngraderTransformer(final TransformerManager transformerManager, final int targetVersion, final Predicate<String> classFilter) {
        this(transformerManager, targetVersion, classFilter, RuntimeDepCollector.NULL);
    }

    JavaDowngraderTransformer(
        TransformerManager transformerManager,
        int targetVersion,
        Predicate<String> classFilter,
        RuntimeDepCollector depCollector
    ) {
        this.transformerManager = transformerManager;
        this.targetVersion = targetVersion;
        this.classFilter = classFilter;
        this.depCollector = depCollector;
    }

    @Override
    public byte[] transform(String className, byte[] bytecode, boolean calculateStackMapFrames) {
        final int majorVersion = (bytecode[6] & 0xFF) << 8 | (bytecode[7] & 0xFF);
        if (majorVersion <= this.targetVersion) {
            return null;
        }
        if (!this.classFilter.test(className)) {
            return null;
        }

        final ClassNode classNode = ASMUtils.fromBytes(bytecode);
        JavaDowngrader.downgrade(classNode, this.targetVersion, this.depCollector);

        if (calculateStackMapFrames) {
            return ASMUtils.toBytes(classNode, this.transformerManager.getClassTree(), this.transformerManager.getClassProvider());
        } else {
            return ASMUtils.toStacklessBytes(classNode);
        }
    }

    public static Builder builder(TransformerManager transformerManager) {
        return new Builder(transformerManager);
    }

    public static final class Builder {
        private final TransformerManager transformerManager;
        private int targetVersion = NATIVE_CLASS_VERSION;
        private Predicate<String> classFilter = c -> true;
        private RuntimeDepCollector depCollector = RuntimeDepCollector.NULL;

        Builder(TransformerManager transformerManager) {
            this.transformerManager = transformerManager;
        }

        public Builder targetVersion(int targetVersion) {
            this.targetVersion = targetVersion;
            return this;
        }

        public Builder classFilter(Predicate<String> classFilter) {
            this.classFilter = classFilter;
            return this;
        }

        public Builder depCollector(RuntimeDepCollector depCollector) {
            this.depCollector = depCollector;
            return this;
        }

        public JavaDowngraderTransformer build() {
            return new JavaDowngraderTransformer(
                transformerManager,
                targetVersion,
                classFilter,
                depCollector
            );
        }
    }

}
