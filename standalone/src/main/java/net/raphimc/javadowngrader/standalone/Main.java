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
package net.raphimc.javadowngrader.standalone;

import joptsimple.*;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import net.raphimc.javadowngrader.standalone.progress.MultiThreadedProgressBar;
import net.raphimc.javadowngrader.standalone.transform.JavaDowngraderTransformer;
import net.raphimc.javadowngrader.standalone.transform.LazyFileClassProvider;
import net.raphimc.javadowngrader.standalone.transform.PathClassProvider;
import net.raphimc.javadowngrader.standalone.util.CloseableSupplier;
import net.raphimc.javadowngrader.standalone.util.GeneralUtil;
import net.raphimc.javadowngrader.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Throwable {
        final OptionParser parser = new OptionParser();
        final OptionSpec<Void> help = parser.acceptsAll(asList("help", "h", "?"), "Get a list of all arguments").forHelp();

        final OptionSpec<File> inputLocation = parser.acceptsAll(asList("input_file", "input", "i"), "The location of the input jar file")
                .withRequiredArg()
                .ofType(File.class)
                .required();
        final OptionSpec<File> outputLocation = parser.acceptsAll(asList("output_file", "output", "o"), "The location of the output jar file")
                .withRequiredArg()
                .ofType(File.class)
                .required();
        final OptionSpec<JavaVersion> version = parser.acceptsAll(asList("target_version", "version", "v"), "The target/output java version")
                .withRequiredArg()
                .withValuesConvertedBy(new JavaVersionEnumConverter())
                .required();
        final OptionSpec<List<File>> libraryPath = parser.acceptsAll(asList("library_path", "library", "l"), "Additional libraries to add to the classpath (required for stack frames)")
                .withRequiredArg()
                .withValuesConvertedBy(new PathConverter());
        final OptionSpec<Integer> threadCount = parser.acceptsAll(asList("thread_count", "threads", "t"), "The number of threads to use for the downgrading")
            .withRequiredArg()
            .ofType(Integer.class)
            .defaultsTo(Math.min(Runtime.getRuntime().availableProcessors(), 255));

        final OptionSet options;
        try {
            options = parser.parse(args);
        } catch (OptionException | ValueConversionException e) {
            LOGGER.error("Error parsing options: " + e.getMessage());
            parser.printHelpOn(System.out);
            System.exit(1);
            return;
        }
        if (options.has(help)) {
            parser.printHelpOn(System.out);
            System.exit(1);
        }

        final File inputFile = options.valueOf(inputLocation);
        if (!inputFile.isFile()) {
            LOGGER.error("Input file does not exist or is not a file");
            System.exit(1);
        }
        if (!inputFile.canRead()) {
            LOGGER.error("Cannot read input file");
            System.exit(1);
        }

        final File outputFile = options.valueOf(outputLocation);
        final File parentFile = outputFile.getParentFile();
        if (parentFile != null) {
            outputFile.getParentFile().mkdirs();
            if (!outputFile.getParentFile().isDirectory()) {
                LOGGER.error("Failed to create output directory");
                System.exit(1);
            }
        }

        try {
            final long start = System.nanoTime();
            doConversion(
                inputFile, outputFile,
                options.valueOf(version),
                GeneralUtil.flatten(options.valuesOf(libraryPath)),
                Math.min(options.valueOf(threadCount), 255)
            );
            final long end = System.nanoTime();
            LOGGER.info(
                    "Done in {}.",
                    Duration.ofNanos(end - start)
                            .toString()
                            .substring(2)
                            .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                            .toLowerCase(Locale.ROOT)
            );
        } catch (Throwable e) {
            LOGGER.error("Error while converting jar file. Please report this issue on the JavaDowngrader GitHub page", e);
            System.exit(1);
        }
    }

    private static void doConversion(
            final File inputFile,
            final File outputFile,
            final JavaVersion targetVersion,
            List<File> libraryPath,
            int threadCount
    ) throws Throwable {
        LOGGER.info("Downgrading {} to Java {}", inputFile, targetVersion.getName());
        if (outputFile.isFile() && !outputFile.canWrite()) {
            LOGGER.error("Cannot write to {}", outputFile);
            System.exit(1);
        }
        if (Files.deleteIfExists(outputFile.toPath())) {
            LOGGER.info("Deleted old {}", outputFile);
        }

        try (Stream<File> stream = libraryPath.stream()
                .flatMap(f -> {
                    if (f.isFile()) {
                        return Stream.of(f);
                    }
                    try {
                        // IntelliJ doesn't understand that this stream then becomes part of the outer stream
                        //noinspection resource
                        return Files.walk(f.toPath())
                                .filter(Files::isRegularFile)
                                .filter(p -> p.toString().endsWith(".jar"))
                                .map(Path::toFile);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
        ) {
            libraryPath = stream.collect(Collectors.toList());
        }

        LOGGER.info("Opening source JAR");
        try (FileSystem inFs = FileSystems.newFileSystem(inputFile.toPath(), null)) {
            final Path inRoot = inFs.getRootDirectories().iterator().next();
            final TransformerManager transformerManager = new TransformerManager(
                    new PathClassProvider(inRoot, new LazyFileClassProvider(libraryPath, new BasicClassProvider()))
            );
            transformerManager.addBytecodeTransformer(new JavaDowngraderTransformer(
                    transformerManager, targetVersion.getVersion(),
                    c -> Files.isRegularFile(inRoot.resolve(GeneralUtil.toClassFilename(c)))
            ));

            try (FileSystem outFs = FileSystems.newFileSystem(new URI("jar:" + outputFile.toURI()), Collections.singletonMap("create", "true"))) {
                final Path outRoot = outFs.getRootDirectories().iterator().next();
                LOGGER.info("Copying runtime classes");
                try (CloseableSupplier<Path, IOException> supplier = GeneralUtil.getPath(Main.class.getResource(
                    '/' + Constants.JAVADOWNGRADER_RUNTIME_PACKAGE + Constants.JAVADOWNGRADER_RUNTIME_ROOT
                ).toURI())) {
                    final Path runtimeRoot = supplier.get().getParent();
                    try (Stream<Path> stream = Files.walk(runtimeRoot)) {
                        stream.filter(Files::isRegularFile)
                            .filter(p -> !p.getFileName().toString().equals(Constants.JAVADOWNGRADER_RUNTIME_ROOT))
                            .forEach(p -> {
                                final String relative = GeneralUtil.slashName(runtimeRoot.relativize(p));
                                final Path dest = outRoot.resolve(Constants.JAVADOWNGRADER_RUNTIME_PACKAGE + relative);
                                try {
                                    Files.createDirectories(dest.getParent());
                                    Files.copy(p, dest);
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            });
                    }
                }
                LOGGER.info("Downgrading classes with {} thread(s)", threadCount);
                final ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
                final List<Callable<Void>> tasks;
                final MultiThreadedProgressBar[] pb = new MultiThreadedProgressBar[1];
                try (Stream<Path> stream = Files.walk(inRoot)) {
                    tasks = stream.map(path -> (Callable<Void>) () -> {
                        final String relative = GeneralUtil.slashName(inRoot.relativize(path));
                        pb[0].setThreadTask(relative);
                        final Path inOther = outRoot.resolve(relative);
                        if (Files.isDirectory(path)) {
                            Files.createDirectories(inOther);
                            pb[0].step();
                            return null;
                        }
                        final Path parent = inOther.getParent();
                        if (parent != null) {
                            Files.createDirectories(parent);
                        }
                        if (!relative.endsWith(".class")) {
                            Files.copy(path, inOther);
                            pb[0].step();
                            return null;
                        }
                        final String className = GeneralUtil.toClassName(relative);
                        final byte[] bytecode = Files.readAllBytes(path);
                        byte[] result = null;
                        try {
                            result = transformerManager.transform(className, bytecode);
                        } catch (Exception e) {
                            LOGGER.error("Failed to transform {}", className, e);
                        }
                        Files.write(inOther, result != null ? result : bytecode);

                        pb[0].step();
                        return null;
                    }).collect(Collectors.toList());
                }
                try {
                    pb[0] = MultiThreadedProgressBar.create(
                        new ProgressBarBuilder()
                            .setTaskName("Downgrading")
                            .setStyle(ProgressBarStyle.ASCII)
                            .setInitialMax(tasks.size())
                            .setUpdateIntervalMillis(100)
                    );
                    threadPool.invokeAll(tasks);
                } finally {
                    if (pb[0] != null) {
                        pb[0].close();
                    }
                }
                threadPool.shutdown();
                if (!threadPool.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                    throw new IllegalStateException("Thread pool didn't shutdown correctly");
                }
                LOGGER.info("Writing final JAR");
            }
        }
    }

}
