package com.purrbyte.ai.service;

import com.purrbyte.ai.util.ZIPHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Extracts JDK source archives and resolves modules to document.
 *
 * <p>Handles downloading and extracting {@code lib/src.zip} from JDK distribution archives,
 * extracting the full source tree with zip-slip protection, and discovering modules by
 * scanning for {@code module-info.java} files.
 */
@Slf4j
@Component
public class SourceExtractor {

    private final Path workDirectory;

    public SourceExtractor(@Value("${doclet.work.directory}") Path workDirectory) {
        this.workDirectory = workDirectory;
    }

    /**
     * Locates the {@code lib/src.zip} of the running JDK.
     */
    public Path localSrcZip() throws IOException {
        Path srcZip = Path.of(System.getProperty("java.home"), "lib", "src.zip");
        if (!Files.exists(srcZip)) {
            throw new IOException("JDK source archive not found at " + srcZip
                    + " — this JDK distribution does not ship lib/src.zip.");
        }
        return srcZip;
    }

    /**
     * Extracts {@code lib/src.zip} out of a downloaded JDK distribution archive ({@code .zip} on
     * Windows, {@code .tar.gz} on Linux/macOS) into {@code <work>/jdk-sources/<version>-src.zip}.
     * Idempotent: an already-extracted file is reused.
     */
    public Path extractSrcZipFromArchive(Path archive, String version) throws IOException {
        Path destSrcZip = workDirectory.resolve("jdk-sources").resolve(version + "-src.zip");
        if (Files.exists(destSrcZip)) {
            return destSrcZip;
        }
        Files.createDirectories(destSrcZip.getParent());
        String name = archive.getFileName().toString().toLowerCase();
        boolean found;
        if (name.endsWith(".zip")) {
            found = extractZipEntry(archive, destSrcZip);
        } else if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
            found = extractTarGzEntry(archive, destSrcZip);
        } else {
            throw new IOException("Unsupported JDK archive type: " + archive.getFileName());
        }
        if (!found) {
            throw new IOException("lib/src.zip not found inside " + archive.getFileName()
                    + " — the distribution may not include sources.");
        }
        log.info("Extracted lib/src.zip from {} to {}", archive.getFileName(), destSrcZip);
        return destSrcZip;
    }

    private boolean extractZipEntry(Path archive, Path dest) throws IOException {
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().replace('\\', '/').endsWith("lib/src.zip")) {
                    try (InputStream in = zip.getInputStream(entry)) {
                        Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private boolean extractTarGzEntry(Path archive, Path dest) throws IOException {
        try (InputStream fileInput = Files.newInputStream(archive);
             GzipCompressorInputStream gzip = new GzipCompressorInputStream(fileInput);
             TarArchiveInputStream tar = new TarArchiveInputStream(gzip)) {
            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().replace('\\', '/').endsWith("lib/src.zip")) {
                    Files.copy(tar, dest, StandardCopyOption.REPLACE_EXISTING);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Extracts a source zip into {@code <work>/jdk-sources/<version>} with zip-slip protection.
     * The extraction is idempotent: if the directory already exists, it is reused.
     *
     * @return true if the source was newly extracted, false if the directory already existed
     */
    public boolean extractSourceZip(Path zipFile, String version, Consumer<Double> progressCallback) throws IOException {
        Path extractDir = workDirectory.resolve("jdk-sources").resolve(version);
        if (Files.exists(extractDir)) {
            log.info("The source has already been obtained at {}.", extractDir);
            return false;
        }
        Files.createDirectories(extractDir);
        int totalEntries;
        try (ZipFile zipFileObj = new ZipFile(zipFile.toFile())) {
            totalEntries = (int) zipFileObj.stream().filter(e -> !e.isDirectory()).count();
        }
        int processed = 0;
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path dest = extractDir.resolve(entry.getName()).normalize();
                if (!dest.startsWith(extractDir)) {
                    log.warn("Skipping zip-slip entry: {}", entry.getName());
                    zis.closeEntry();
                    continue;
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(dest);
                } else {
                    Files.createDirectories(dest.getParent());
                    Files.copy(zis, dest, StandardCopyOption.REPLACE_EXISTING);
                    processed++;
                    if (progressCallback != null && totalEntries > 0) {
                        progressCallback.accept(processed / (double) totalEntries * 100.0);
                    }
                }
                zis.closeEntry();
            }
        }
        log.info("JDK source extracted to: {}", extractDir);
        return true;
    }

    /**
     * Resolves the modules to document: the configured list if any, otherwise every module found under
     * the source root (a directory is a module when it contains a {@code module-info.java}).
     */
    public List<String> resolveModules(Path moduleRoot, List<String> configuredModules) throws IOException {
        if (!configuredModules.isEmpty()) {
            return configuredModules;
        }
        List<String> modules = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(moduleRoot)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry) && Files.exists(entry.resolve("module-info.java"))) {
                    modules.add(entry.getFileName().toString());
                }
            }
        }
        Collections.sort(modules);
        return modules;
    }

    /**
     * Parses a comma-separated module list, trimming blanks. An empty value means "all modules".
     */
    public static List<String> parseModules(String modulesCsv) {
        if (modulesCsv == null || modulesCsv.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(modulesCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
