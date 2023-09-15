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
package net.raphimc.javadowngrader.bootstrap;

import net.raphimc.javadowngrader.util.JavaVersion;

public class JavaVersionSpoofer {

    public static void modifyProperties() {
        if (System.getProperty("spoofJavaVersion") != null) {
            final JavaVersion spoofedJavaVersion = JavaVersion.getByName(System.getProperty("spoofJavaVersion"));
            if (spoofedJavaVersion == null) {
                System.err.println("Unable to find version '" + System.getProperty("spoofJavaVersion") + "'");
                System.exit(-1);
            }
            System.setProperty("java.version", spoofedJavaVersion.getFakeJavaVersionName());
            System.setProperty("java.class.version", String.valueOf(spoofedJavaVersion.getVersion()));
            System.setProperty("java.specification.version", spoofedJavaVersion.getFakeSpecificationVersionName());
        }
    }

}
