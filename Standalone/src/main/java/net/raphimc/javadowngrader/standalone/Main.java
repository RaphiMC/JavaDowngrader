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
import net.raphimc.javadowngrader.JavaDowngrader;
import net.raphimc.javadowngrader.standalone.transform.JarClassProvider;
import net.raphimc.javadowngrader.standalone.transform.JavaDowngraderTransformer;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ParallelScatterZipCreator;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.Deflater;

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
            doConversion(inputFile, outputFile, options.valueOf(version));
        } catch (Throwable e) {
            JavaDowngrader.LOGGER.error("Error while converting jar file. Please report this issue on the JavaDowngrader GitHub page", e);
            System.exit(1);
        }
    }

    private static void doConversion(final File inputFile, final File outputFile, final JavaVersion targetVersion) throws IOException, ExecutionException, InterruptedException {
        final Map<String, byte[]> classes = new HashMap<>();
        final Map<String, byte[]> output = new HashMap<>();

        JavaDowngrader.LOGGER.info("Reading input file");
        final ZipFile zipFile = new ZipFile(inputFile);
        final Enumeration<? extends ZipArchiveEntry> entries = zipFile.getEntries();
        while (entries.hasMoreElements()) {
            final ZipArchiveEntry zipEntry = entries.nextElement();
            if (zipEntry.getName().endsWith("/")) continue;
            final byte[] bytes = new byte[(int) zipEntry.getSize()];
            IOUtils.readFully(zipFile.getInputStream(zipEntry), bytes);
            if (zipEntry.getName().toLowerCase().endsWith(".class")) {
                final String name = zipEntry.getName().substring(0, zipEntry.getName().length() - 6).replace('/', '.');
                classes.put(name, bytes);
            } else {
                output.put(zipEntry.getName(), bytes);
            }
        }
        zipFile.close();

        JavaDowngrader.LOGGER.info("Downgrading classes to Java " + targetVersion.getName());
        final TransformerManager transformerManager = new TransformerManager(new JarClassProvider(classes));
        transformerManager.addBytecodeTransformer(new JavaDowngraderTransformer(transformerManager, targetVersion.getVersion()));

        for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
            final String zipName = entry.getKey().replace('.', '/') + ".class";
            final byte[] result = transformerManager.transform(entry.getKey(), entry.getValue());
            if (result != null) {
                output.put(zipName, result);
            } else {
                output.put(zipName, entry.getValue());
            }
        }

        JavaDowngrader.LOGGER.info("Writing output file");
        final ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        final FileOutputStream fos = new FileOutputStream(outputFile);
        final JarArchiveOutputStream jos = new JarArchiveOutputStream(fos);
        final ParallelScatterZipCreator fastZip = new ParallelScatterZipCreator(threadPool);
        jos.setMethod(Deflater.DEFLATED);
        jos.setLevel(Deflater.BEST_COMPRESSION);

        for (Map.Entry<String, byte[]> entry : output.entrySet()) {
            final JarArchiveEntry jarEntry = new JarArchiveEntry(entry.getKey());
            jarEntry.setMethod(JarArchiveEntry.DEFLATED);
            fastZip.addArchiveEntry(jarEntry, () -> new ByteArrayInputStream(entry.getValue()));
        }

        fastZip.writeTo(jos);
        jos.flush();
        jos.close();
        threadPool.shutdown();
        JavaDowngrader.LOGGER.info("Done");
    }

}
