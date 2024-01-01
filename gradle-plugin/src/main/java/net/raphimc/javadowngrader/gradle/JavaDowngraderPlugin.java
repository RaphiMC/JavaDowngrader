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
package net.raphimc.javadowngrader.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;

public class JavaDowngraderPlugin implements Plugin<Project> {

    @Override
    public void apply(@NotNull Project project) {
        if (GradleVersion.current().compareTo(GradleVersion.version("8.1")) < 0) {
            throw new IllegalStateException("JavaDowngrader requires Gradle 8.1 or newer");
        }
    }

}
