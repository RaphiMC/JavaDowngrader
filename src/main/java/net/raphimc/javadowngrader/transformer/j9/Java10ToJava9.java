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

import net.raphimc.javadowngrader.transformer.DowngradingTransformer;
import net.raphimc.javadowngrader.transformer.j9.methodcallreplacer.*;
import org.objectweb.asm.Opcodes;

public class Java10ToJava9 extends DowngradingTransformer {

    public Java10ToJava9() {
        super(Opcodes.V10, Opcodes.V9);

        this.addMethodCallReplacer(Opcodes.INVOKESTATIC, "java/util/List", "copyOf", "(Ljava/util/Collection;)Ljava/util/List;", new ListCopyOfMCR());
        this.addMethodCallReplacer(Opcodes.INVOKESTATIC, "java/util/Set", "copyOf", "(Ljava/util/Collection;)Ljava/util/Set;", new SetCopyOfMCR());
        this.addMethodCallReplacer(Opcodes.INVOKESTATIC, "java/util/Map", "copyOf", "(Ljava/util/Map;)Ljava/util/Map;", new MapCopyOfMCR());

        this.addMethodCallReplacer(Opcodes.INVOKESTATIC, "java/util/stream/Collectors", "toUnmodifiableMap", "(Ljava/util/function/Function;Ljava/util/function/Function;)Ljava/util/stream/Collector;", new CollectorsToUnmodifiableMapMCR());
        this.addMethodCallReplacer(Opcodes.INVOKESTATIC, "java/util/stream/Collectors", "toUnmodifiableList", "()Ljava/util/stream/Collector;", new CollectorsToUnmodifiableListMCR());
        this.addMethodCallReplacer(Opcodes.INVOKESTATIC, "java/util/stream/Collectors", "toUnmodifiableSet", "()Ljava/util/stream/Collector;", new CollectorsToUnmodifiableSetMCR());

        this.addMethodCallReplacer(Opcodes.INVOKESTATIC, "java/io/Reader", "transferTo", new ReaderTransferToMCR());

        this.addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "orElseThrow", "()Ljava/lang/Object;", new OptionalOrElseThrowMCR());
    }

}
