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
package net.raphimc.javadowngrader.coveragescanner;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;

public class CoverageScanner implements Closeable {
    @Nullable
    private final CtSym ct;

    private final Map<String, Integer> classVersionCache = new ConcurrentHashMap<>();
    private final Map<String, ClassInfo> classInfoCache = new ConcurrentHashMap<>();

    public CoverageScanner(@Nullable Path ctSymPath) throws IOException {
        ct = ctSymPath != null ? CtSym.open(ctSymPath) : null;
    }

    /**
     * @apiNote {@code handler} may be called from multiple threads
     */
    @SuppressWarnings("resource") // Handled by iterStream
    public void scanJar(Path jarPath, ScanHandler handler, @Nullable Integer baseJava) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(jarPath, null)) {
            final Path root = fs.getRootDirectories().iterator().next();
            IOUtil.iterStream(
                Files.walk(root)
                    .parallel()
                    .filter(p -> p.toString().endsWith(".class"))
                    .filter(Files::isRegularFile),
                path -> scanClass(path, handler, baseJava)
            );
        }
    }

    public void scanClass(Path classFilePath, ScanHandler handler, @Nullable Integer baseJava) throws IOException {
        final ClassReader reader;
        try (InputStream is = Files.newInputStream(classFilePath)) {
            reader = new ClassReader(is);
        }
        scanClass(reader, handler, baseJava);
    }

    public void scanClass(ClassReader reader, ScanHandler handler, @Nullable Integer baseJava) {
        final int javaVersion = baseJava != null
            ? baseJava
            : reader.readInt(reader.getItem(1) - 7) - 44; // Match ClassReader.accept
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            MethodLocation classLocation;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                classLocation = new MethodLocation(name.replace('/', '.'), null, javaVersion);
                checkSignature(classLocation, handler, signature, false);
                if (superName != null) {
                    checkType(classLocation, handler, Type.getObjectType(superName));
                }
                if (interfaces != null) {
                    for (final String intf : interfaces) {
                        checkType(classLocation, handler, Type.getObjectType(intf));
                    }
                }
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                return checkAnnotation(classLocation, handler, descriptor, visible);
            }

            @Override
            public void visitInnerClass(String name, String outerName, String innerName, int access) {
                checkType(classLocation, handler, Type.getObjectType(name));
                if (outerName != null) {
                    checkType(classLocation, handler, Type.getObjectType(outerName));
                }
            }

            @Override
            public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
                return checkAnnotation(classLocation, handler, descriptor, visible);
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                checkType(classLocation, handler, Type.getType(descriptor));
                checkSignature(classLocation, handler, signature, true);
                return new FieldVisitor(api) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                        return checkAnnotation(classLocation, handler, descriptor, visible);
                    }

                    @Override
                    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
                        return checkAnnotation(classLocation, handler, descriptor, visible);
                    }
                };
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                final MethodLocation methodLocation = new MethodLocation(classLocation.inClass, name, classLocation.inJava);
                checkType(methodLocation, handler, Type.getType(descriptor));
                checkSignature(methodLocation, handler, signature, false);
                if (exceptions != null) {
                    for (final String exc : exceptions) {
                        checkType(methodLocation, handler, Type.getObjectType(exc));
                    }
                }
                // TODO
                return new MethodVisitor(api) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                        return checkAnnotation(methodLocation, handler, descriptor, visible);
                    }

                    @Override
                    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
                        return checkAnnotation(methodLocation, handler, descriptor, visible);
                    }

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                        checkMember(methodLocation, handler, owner, name, descriptor, false);
                    }

                    @Override
                    public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
                        return checkAnnotation(methodLocation, handler, descriptor, visible);
                    }

                    @Override
                    public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
                        checkType(methodLocation, handler, Type.getType(descriptor));
                    }

                    @Override
                    public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
                        return checkAnnotation(methodLocation, handler, descriptor, visible);
                    }

                    @Override
                    public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
                        return checkAnnotation(methodLocation, handler, descriptor, visible);
                    }

                    @Override
                    public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
                        return checkAnnotation(methodLocation, handler, descriptor, visible);
                    }

                    @Override
                    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
                        checkType(methodLocation, handler, Type.getType(descriptor));
                        checkObject(methodLocation, handler, bootstrapMethodHandle);
                        for (final Object arg : bootstrapMethodArguments) {
                            checkObject(methodLocation, handler, arg);
                        }
                    }

                    @Override
                    public void visitLdcInsn(Object value) {
                        checkObject(methodLocation, handler, value);
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        checkMember(methodLocation, handler, owner, name, descriptor, true);
                    }

                    @Override
                    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
                        checkType(methodLocation, handler, Type.getType(descriptor));
                    }

                    @Override
                    public void visitTryCatchBlock(Label start, Label end, Label handlerLabel, String type) {
                        if (type != null) {
                            checkType(methodLocation, handler, Type.getObjectType(type));
                        }
                    }

                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        checkType(methodLocation, handler, Type.getObjectType(type));
                    }
                };
            }

            @Override
            public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
                checkType(classLocation, handler, Type.getType(descriptor));
                checkSignature(classLocation, handler, signature, false);
                return new RecordComponentVisitor(api) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                        return checkAnnotation(classLocation, handler, descriptor, visible);
                    }

                    @Override
                    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
                        return checkAnnotation(classLocation, handler, descriptor, visible);
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

    private void checkSignature(MethodLocation location, ScanHandler handler, String signature, boolean isField) {
        if (signature == null) return;
        final SignatureVisitor visitor = new SignatureVisitor(Opcodes.ASM9) {
            @Override
            public void visitClassType(String name) {
                checkType(location, handler, Type.getObjectType(name));
            }
        };
        if (isField) {
            new SignatureReader(signature).acceptType(visitor);
        } else {
            new SignatureReader(signature).accept(visitor);
        }
    }

    private AnnotationVisitor checkAnnotation(
        MethodLocation location, ScanHandler handler, String descriptor, boolean visible
    ) {
        if (!visible) {
            // Invisible annotations can be in the class file without issue
            return null;
        }
        checkType(location, handler, Type.getType(descriptor));
        return new AnnotationVisitor(Opcodes.ASM9) {
            @Override
            public void visit(String name, Object value) {
                checkObject(location, handler, value);
            }

            @Override
            public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                checkType(location, handler, Type.getType(descriptor));
                return this;
            }

            @Override
            public AnnotationVisitor visitArray(String name) {
                return this;
            }

            @Override
            public void visitEnum(String name, String descriptor, String value) {
                final Type type = Type.getType(descriptor);
                checkType(location, handler, type);
                checkMember(
                    location, handler,
                    Type.getType(descriptor).getInternalName(), value, descriptor,
                    false
                );
            }
        };
    }

    private void checkObject(MethodLocation location, ScanHandler handler, Object obj) {
        if (obj instanceof Handle) {
            final Handle handle = (Handle)obj;
            checkMember(
                location, handler,
                handle.getOwner(), handle.getName(), handle.getDesc(),
                handle.getTag() >= Opcodes.INVOKEVIRTUAL
            );
        }
        if (obj instanceof ConstantDynamic) {
            final ConstantDynamic condy = (ConstantDynamic)obj;
            checkType(location, handler, Type.getType(condy.getDescriptor()));
            checkObject(location, handler, condy.getBootstrapMethod());
        }
    }

    private void checkType(MethodLocation location, ScanHandler handler, Type type) {
        if (type.getSort() == Type.METHOD) {
            for (final Type arg : type.getArgumentTypes()) {
                checkType(location, handler, arg);
            }
            checkType(location, handler, type.getReturnType());
            return;
        }
        if (type.getSort() == Type.ARRAY) {
            checkType(location, handler, type.getElementType());
            return;
        }
        if (type.getSort() != Type.OBJECT) return;

        final String className = type.getClassName();

        final Integer cached = classVersionCache.get(className);
        if (cached != null) {
            if (cached > location.inJava) {
                handler.missing(location, new MethodLocation(className, null, cached));
            }
            return;
        }

        if (ct != null) {
            final SortedMap<Integer, Path> versions = ct.getVersions(className);
            if (versions != null) {
                final int minVersion = versions.firstKey();
                classVersionCache.put(className, minVersion);
                if (minVersion > location.inJava) {
                    handler.missing(location, new MethodLocation(className, null, minVersion));
                }
                return;
            }
        }

        if (!className.startsWith("java.") && !className.startsWith("jdk.")) return;

        final URL classUrl = ClassLoader.getSystemResource(className.replace('.', '/').concat(".class"));
        if (classUrl == null) {
            handler.missing(location, new MethodLocation(className, null, 0));
        } else {
            classVersionCache.put(className, 0);
        }
    }

    private void checkMember(
        MethodLocation location, ScanHandler handler,
        String owner, String name, String descriptor,
        boolean isMethod
    ) {
        final Type ownerType = Type.getObjectType(owner);
        checkType(location, handler, ownerType);
        // No need to check descriptor

        if (ct == null) return;

        final String className = ownerType.getClassName();
        final Integer addedVersion = classVersionCache.get(className);
        if (addedVersion == null || addedVersion == 0 || addedVersion > location.inJava) return;

        final ClassInfo classInfo = classInfoCache.computeIfAbsent(className, this::constructClassInfo);
        final Map<NameAndType, Integer> members = isMethod ? classInfo.methods : classInfo.fields;
        final Integer memberAdded = members.get(new NameAndType(name, Type.getType(descriptor)));
        if (memberAdded != null && memberAdded > location.inJava) {
            // Unfortunately there's no good way to differentiate between something that was added in this Java and
            // something that's always been there
            handler.missing(location, new MethodLocation(className, name, memberAdded));
        }
    }

    private ClassInfo constructClassInfo(String className) {
        assert ct != null;
        final ClassInfo result = new ClassInfo();
        final SortedMap<Integer, Path> files = ct.getVersions(className);
        if (files == null) {
            return result;
        }
        for (final Map.Entry<Integer, Path> file : files.entrySet()) {
            final ClassReader reader;
            try (InputStream is = Files.newInputStream(file.getValue())) {
                reader = new ClassReader(is);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            reader.accept(new ClassVisitor(Opcodes.ASM9) {
                private void add(Map<NameAndType, Integer> members, String name, String descriptor) {
                    members.putIfAbsent(new NameAndType(name, Type.getType(descriptor)), file.getKey());
                }

                @Override
                public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                    add(result.fields, name, descriptor);
                    return null;
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    add(result.methods, name, descriptor);
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        if (ct != null) {
            ct.close();
        }
    }

    private static class ClassInfo {
        final Map<NameAndType, Integer> fields = new HashMap<>();
        final Map<NameAndType, Integer> methods = new HashMap<>();
    }

    private static class NameAndType {
        final String name;
        final Type type;

        NameAndType(String name, Type type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String toString() {
            return name + (type.getSort() != Type.METHOD ? ":" : "") + type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NameAndType)) return false;
            NameAndType that = (NameAndType)o;
            return Objects.equals(name, that.name) && Objects.equals(type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type);
        }
    }

    @FunctionalInterface
    public interface ScanHandler {
        void missing(MethodLocation location, MethodLocation missing);
    }

    public static class MethodLocation {
        private final String inClass;
        @Nullable
        private final String inMethod;
        private final int inJava;

        public MethodLocation(String inClass, @Nullable String inMethod, int inJava) {
            this.inClass = inClass;
            this.inMethod = inMethod;
            this.inJava = inJava;
        }

        public String getInClass() {
            return inClass;
        }

        @Nullable
        public String getInMethod() {
            return inMethod;
        }

        public int getInJava() {
            return inJava;
        }

        @Override
        public String toString() {
            return "MethodLocation{" +
                "inClass='" + inClass + '\'' +
                ", inMethod='" + inMethod + '\'' +
                ", inJava=" + inJava +
                '}';
        }
    }
}
