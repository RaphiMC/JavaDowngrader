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
package net.raphimc.javadowngrader.standalone;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.raphimc.javadowngrader.JavaDowngrader;
import net.raphimc.javadowngrader.standalone.transform.JavaDowngraderTransformer;
import net.raphimc.javadowngrader.standalone.transform.PathClassProvider;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.Locale;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class Main {

    public static void main(String[] args) throws Throwable {
        final OptionParser parser = new OptionParser();
        final OptionSpec<Void> help = parser.acceptsAll(asList("help", "h", "?"), "Get a list of all arguments").forHelp();

        final OptionSpec<String> inputLocation = parser.acceptsAll(asList("input_file", "input", "i"), "The location of the input jar file").withRequiredArg().ofType(String.class).required();
        final OptionSpec<String> outputLocation = parser.acceptsAll(asList("output_file", "output", "o"), "The location of the output jar file").withRequiredArg().ofType(String.class).required();
        final OptionSpec<JavaVersion> version = parser.acceptsAll(asList("target_version", "version", "v"), "The target/output java version").withRequiredArg().withValuesConvertedBy(new JavaVersionEnumConverter()).required();

        final OptionSet options;
        try {
            options = parser.parse(args);
        } catch (OptionException e) {
            JavaDowngrader.LOGGER.error("Error parsing options: " + e.getMessage());
            parser.printHelpOn(System.out);
            System.exit(1);
            return;
        }
        if (options.has(help)) {
            parser.printHelpOn(System.out);
            System.exit(1);
        }

        final File inputFile = new File(options.valueOf(inputLocation));
        if (!inputFile.isFile()) {
            JavaDowngrader.LOGGER.error("Input file does not exist or is not a file");
            System.exit(1);
        }

        final File outputFile = new File(options.valueOf(outputLocation));
        final File parentFile = outputFile.getParentFile();
        if (parentFile != null) {
            outputFile.getParentFile().mkdirs();
            if (!outputFile.getParentFile().isDirectory()) {
                JavaDowngrader.LOGGER.error("Failed to create output directory");
                System.exit(1);
            }
        }

        try {
            final long start = System.nanoTime();
            doConversion(inputFile, outputFile, options.valueOf(version));
            final long end = System.nanoTime();
            JavaDowngrader.LOGGER.info(
                "Done in {}.",
                Duration.ofNanos(end - start)
                    .toString()
                    .substring(2)
                    .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                    .toLowerCase(Locale.ROOT)
            );
        } catch (Throwable e) {
            JavaDowngrader.LOGGER.error("Error while converting jar file. Please report this issue on the JavaDowngrader GitHub page", e);
            System.exit(1);
        }
    }

    private static void doConversion(final File inputFile, final File outputFile, final JavaVersion targetVersion) throws IOException, URISyntaxException {
        JavaDowngrader.LOGGER.info("Downgrading classes to Java {}", targetVersion.getName());
        if (outputFile.delete()) {
            JavaDowngrader.LOGGER.info("Deleted old {}", outputFile);
        }

        try (FileSystem inFs = FileSystems.newFileSystem(inputFile.toPath(), null)) {
            final Path inRoot = inFs.getRootDirectories().iterator().next();
            final TransformerManager transformerManager = new TransformerManager(new PathClassProvider(inRoot));
            transformerManager.addBytecodeTransformer(new JavaDowngraderTransformer(
                transformerManager, targetVersion.getVersion(),
                c -> Files.isRegularFile(inRoot.resolve(ASMUtils.slash(c).concat(".class")))
            ));

            try (FileSystem outFs = FileSystems.newFileSystem(new URI("jar:" + outputFile.toURI()), Collections.singletonMap("create", "true"))) {
                final Path outRoot = outFs.getRootDirectories().iterator().next();
                try (Stream<Path> stream = Files.walk(inRoot)) {
                    stream.parallel().forEach(path -> {
                        try {
                            final String relative = inRoot.relativize(path).toString();
                            final Path inOther = outRoot.resolve(relative);
                            if (Files.isDirectory(path)) {
                                Files.createDirectories(inOther);
                                return;
                            }
                            final Path parent = inOther.getParent();
                            if (parent != null) {
                                Files.createDirectories(parent);
                            }
                            if (!relative.endsWith(".class")) {
                                Files.copy(path, inOther);
                                return;
                            }
                            final String className = ASMUtils.dot(relative.substring(0, relative.length() - 6));
                            final byte[] bytecode = Files.readAllBytes(path);
                            final byte[] result = transformerManager.transform(className, bytecode);
                            Files.write(inOther, result != null ? result : bytecode);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
                }
            }
        }
    }

}
