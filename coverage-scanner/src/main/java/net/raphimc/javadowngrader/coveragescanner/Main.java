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

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.annotation.Arg;
import net.sourceforge.argparse4j.ext.java7.PathArgumentType;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderNotFoundException;
import java.util.List;

public class Main {
    public static class ParsedArgs {
        @Arg(dest = "java_home")
        public Path javaHome;

        @Arg(dest = "base_java")
        @Nullable
        public Integer baseJava;

        @Arg(dest = "inputs")
        public List<Path> inputs;

        @Override
        public String toString() {
            return "ParsedArgs{" +
                "javaHome=" + javaHome +
                ", baseJava=" + baseJava +
                ", inputs=" + inputs +
                '}';
        }
    }

    public static void main(String[] args) {
        final ArgumentParser parser = ArgumentParsers.newFor("coverage-scanner").build()
            .defaultHelp(true)
            .description("Shows a list of JDK method calls that failed to downgrade");
        parser.addArgument("-j", "--java-home")
            .help("Alternate path to a Java installation (used for ct.sym)")
            .type(new PathArgumentType()
                .verifyCanRead()
                .verifyIsDirectory()
            )
            .setDefault(Paths.get(System.getProperty("java.home")));
        parser.addArgument("-b", "--base-java")
            .help("The version of Java to check from. Default is the versions of the classes themselves.")
            .type(int.class);
        parser.addArgument("inputs")
            .help("Jars/classes to test coverage for")
            .nargs("+")
            .type(new PathArgumentType()
                .verifyCanRead()
                .verifyIsFile()
            );

        final ParsedArgs parsedArgs = new ParsedArgs();
        try {
            parser.parseArgs(args, parsedArgs);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        run(parsedArgs);
    }

    public static void run(ParsedArgs args) {
        Path ctSymPath = args.javaHome.resolve("lib/ct.sym");
        if (!Files.isRegularFile(ctSymPath)) {
            System.err.println("WARNING: ct.sym not found, scanning support will be limited");
            ctSymPath = null;
        }

        final long ctStartTime = System.nanoTime();
        try (CoverageScanner scanner = new CoverageScanner(ctSymPath)) {
            final long ctEndTime = System.nanoTime();
            System.err.println("INFO: Loaded ct.sym in " + (ctEndTime - ctStartTime) / 1000 / 1000.0 + "ms");

            for (final Path jar : args.inputs) {
                System.err.println("INFO: Starting scan of " + jar);
                final long startTime = System.nanoTime();
                try {
                    scanner.scanJar(jar, Main::missingHandler, args.baseJava);
                } catch (ProviderNotFoundException e) {
                    // Probably a standalone class file
                    scanner.scanClass(jar, Main::missingHandler, args.baseJava);
                }
                final long endTime = System.nanoTime();
                System.err.println("INFO: Scanned " + jar + " in " + (endTime - startTime) / 1000 / 1000.0 + "ms");
            }
        } catch (IOException e) {
            System.err.println("Failed to scan due to unexpected error");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void missingHandler(CoverageScanner.MemberLocation location, CoverageScanner.MemberLocation missing) {
        final StringBuilder message = new StringBuilder();
        message.append(location.getInClass());
        if (location.getInMember() != null) {
            message.append('.').append(location.getInMember());
        }
        message.append(" uses ").append(missing.getInClass());
        if (missing.getInMember() != null) {
            message.append('.').append(missing.getInMember());
        }
        message.append(", which is not present in Java ").append(location.getInJava()).append('.');
        if (missing.getInJava() > 0) {
            message.append(" It was added in Java ").append(missing.getInJava()).append('.');
        }
        System.out.println(message);
    }
}
