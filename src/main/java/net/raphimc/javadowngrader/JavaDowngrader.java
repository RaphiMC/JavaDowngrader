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
package net.raphimc.javadowngrader;

import net.raphimc.javadowngrader.transformer.DowngradeResult;
import net.raphimc.javadowngrader.transformer.DowngradingTransformer;
import net.raphimc.javadowngrader.transformer.j10.Java11ToJava10;
import net.raphimc.javadowngrader.transformer.j11.Java12ToJava11;
import net.raphimc.javadowngrader.transformer.j12.Java13ToJava12;
import net.raphimc.javadowngrader.transformer.j13.Java14ToJava13;
import net.raphimc.javadowngrader.transformer.j14.Java15ToJava14;
import net.raphimc.javadowngrader.transformer.j15.Java16ToJava15;
import net.raphimc.javadowngrader.transformer.j16.Java17ToJava16;
import net.raphimc.javadowngrader.transformer.j17.Java18ToJava17;
import net.raphimc.javadowngrader.transformer.j18.Java19ToJava18;
import net.raphimc.javadowngrader.transformer.j19.Java20ToJava19;
import net.raphimc.javadowngrader.transformer.j20.Java21ToJava20;
import net.raphimc.javadowngrader.transformer.j8.Java9ToJava8;
import net.raphimc.javadowngrader.transformer.j9.Java10ToJava9;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.List;

public class JavaDowngrader {

    private static final List<DowngradingTransformer> TRANSFORMER = new ArrayList<>();

    static {
        TRANSFORMER.add(new Java21ToJava20());
        TRANSFORMER.add(new Java20ToJava19());
        TRANSFORMER.add(new Java19ToJava18());
        TRANSFORMER.add(new Java18ToJava17());
        TRANSFORMER.add(new Java17ToJava16());
        TRANSFORMER.add(new Java16ToJava15());
        TRANSFORMER.add(new Java15ToJava14());
        TRANSFORMER.add(new Java14ToJava13());
        TRANSFORMER.add(new Java13ToJava12());
        TRANSFORMER.add(new Java12ToJava11());
        TRANSFORMER.add(new Java11ToJava10());
        TRANSFORMER.add(new Java10ToJava9());
        TRANSFORMER.add(new Java9ToJava8());
    }

    /**
     * Downgrades the given class to the target version
     *
     * @param classNode     The class to downgrade
     * @param targetVersion The target Java version
     * @return The {@link DowngradeResult}
     * @see ClassNode
     */
    public static DowngradeResult downgrade(final ClassNode classNode, final int targetVersion) {
        return downgrade(classNode, targetVersion, RuntimeDepCollector.NULL);
    }

    /**
     * Downgrades the given class to the target version
     *
     * @param classNode     The class to downgrade
     * @param targetVersion The target Java version
     * @param depCollector  The {@link RuntimeDepCollector} to use to collect runtime dependencies. Check the javadoc
     *                      of {@link RuntimeDepCollector} for more info.
     * @return The {@link DowngradeResult}
     * @see ClassNode
     * @see RuntimeDepCollector
     */
    public static DowngradeResult downgrade(final ClassNode classNode, final int targetVersion, final RuntimeDepCollector depCollector) {
        final DowngradeResult result = new DowngradeResult();
        for (DowngradingTransformer transformer : TRANSFORMER) {
            if (transformer.getTargetVersion() >= targetVersion && (classNode.version & 0xFF) > transformer.getTargetVersion()) {
                transformer.transform(classNode, depCollector, result);
            }
        }
        return result;
    }

}
