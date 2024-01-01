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
package net.raphimc.javadowngrader.util;

import org.objectweb.asm.Opcodes;

public enum JavaVersion {

    JAVA_8("8", Opcodes.V1_8),
    JAVA_9("9", Opcodes.V9),
    JAVA_10("10", Opcodes.V10),
    JAVA_11("11", Opcodes.V11),
    JAVA_12("12", Opcodes.V12),
    JAVA_13("13", Opcodes.V13),
    JAVA_14("14", Opcodes.V14),
    JAVA_15("15", Opcodes.V15),
    JAVA_16("16", Opcodes.V16),
    JAVA_17("17", Opcodes.V17),
    JAVA_18("18", Opcodes.V18),
    JAVA_19("19", Opcodes.V19),
    JAVA_20("20", Opcodes.V20),
    JAVA_21("21", Opcodes.V21),
    ;

    private final String name;
    private final int version;

    JavaVersion(final String name, final int version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return this.name;
    }

    public int getVersion() {
        return this.version;
    }

    public String getFakeJavaVersionName() {
        if (this.ordinal() <= JAVA_8.ordinal()) {
            return "1." + this.name + ".0";
        } else {
            return this.version + ".0.0";
        }
    }

    public String getFakeSpecificationVersionName() {
        if (this.ordinal() <= JAVA_8.ordinal()) {
            return "1." + this.name;
        } else {
            return this.name;
        }
    }

    public static JavaVersion getByName(String name) {
        name = name.toLowerCase().replace("java", "").replace("j", "");
        for (JavaVersion version : JavaVersion.values()) {
            if (version.getName().equalsIgnoreCase(name)) return version;
        }
        return null;
    }

}
