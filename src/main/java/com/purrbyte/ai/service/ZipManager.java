package com.purrbyte.ai.service;

import com.purrbyte.ai.util.ZIPHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.zip.ZipFile;

/**
 * Manages ZIP operations for JDK documentation versions.
 *
 * <p>Handles compressing version directories to ZIP files, finding version ZIPs in the output
 * directory, and verifying ZIP contents. All operations are scoped under the configured data directory.
 */
@Slf4j
@Component
public class ZipManager {

    private final Path outputDirectory;

    public ZipManager(@Value("${data.directory}") Path outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    /**
     * Compresses the version directory into a ZIP file at {@code data/jdk/<version>.zip}
     * and deletes the original extracted directory. Idempotent: skips if ZIP already exists.
     *
     * @param versionDir the extracted version directory to compress (e.g. {@code data/jdk/25.0.3/})
     * @param version    the version string (used for ZIP filename)
     */
    public void zipVersion(Path versionDir, String version) throws IOException {
        Path zipPath = versionDir.getParent().resolve(version + ".zip");
        if (Files.exists(zipPath)) {
            log.info("ZIP already exists at {}, cleaning up directory", zipPath);
            deleteDirectory(versionDir);
            return;
        }
        log.info("Compressing {} into {}", versionDir, zipPath);
        try (var zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(zipPath))) {
            try (var walk = Files.walk(versionDir)) {
                String versionPrefix = version + "/";
                walk.forEach(source -> {
                    try {
                        String entryName = versionDir.relativize(source).toString();
                        if (Files.isDirectory(source)) {
                            zos.putNextEntry(new java.util.zip.ZipEntry(versionPrefix + entryName + "/"));
                            zos.closeEntry();
                        } else {
                            zos.putNextEntry(new java.util.zip.ZipEntry(versionPrefix + entryName));
                            Files.copy(source, zos);
                            zos.closeEntry();
                        }
                    } catch (IOException e) {
                        throw new java.io.UncheckedIOException(e);
                    }
                });
            }
        }
        // Delete the original directory after successful compression
        try (var walk = Files.walk(versionDir)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            log.warn("Failed to delete {}: {}", p, e.getMessage());
                        }
                    });
        }
        log.info("Compression complete: {}", zipPath);
    }

    /**
     * Finds a version's documentation ZIP file under the output directory.
     * Searches recursively for a file named {@code <version>.zip}.
     *
     * @param version JDK version
     * @return path to the ZIP file, or null if not found
     */
    public Path findVersionZip(String version) {
        Path jdkDir = outputDirectory.resolve("jdk");
        if (!Files.isDirectory(jdkDir)) {
            return null;
        }
        try (var stream = Files.walk(jdkDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(version + ".zip"))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            log.warn("Failed to list JDK directory {}: {}", jdkDir, e.getMessage());
            return null;
        }
    }

    /**
     * Deletes a directory and all its contents recursively.
     */
    private void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (var walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            log.warn("Failed to delete {}: {}", p, e.getMessage());
                        }
                    });
        }
    }

    /**
     * Checks if documentation has been generated for the specified version.
     * Searches recursively under the configured data directory.
     *
     * @param version JDK version
     * @return true if <version>.zip exists and contains index.json
     */
    public boolean isVersionGenerated(String version) {
        Path zipPath = findVersionZip(version);
        if (zipPath == null) {
            return false;
        }
        try (ZipFile zf = new ZipFile(zipPath.toFile())) {
            return ZIPHelper.findZipEntry(zf, "index.json") != null;
        } catch (IOException e) {
            log.warn("Failed to read ZIP {}: {}", zipPath, e.getMessage());
            return false;
        }
    }
}
