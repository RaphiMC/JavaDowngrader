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
package net.raphimc.javadowngrader.transformer.j16;

import net.raphimc.javadowngrader.transformer.DowngradingTransformer;
import net.raphimc.javadowngrader.transformer.j16.methodcallreplacer.RandomGeneratorNextLongMCR;
import org.objectweb.asm.Opcodes;

public class Java17ToJava16 extends DowngradingTransformer {
    public static final String RANDOM_SUPPORT = "net/raphimc/javadowngrader/runtime/jdk/internal/util/random/RandomSupport";

    public Java17ToJava16() {
        super(Opcodes.V17, Opcodes.V16);

        final String[] randomClasses = {
            "java/util/Random",
            "java/security/SecureRandom",
            "java/util/concurrent/ThreadLocalRandom"
        };
        for (final String clazz : randomClasses) {
            addMethodCallReplacer(Opcodes.INVOKEVIRTUAL, clazz, "nextLong", "(J)J", new RandomGeneratorNextLongMCR());
        }
    }

}
