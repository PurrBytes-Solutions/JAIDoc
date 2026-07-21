package com.purrbyte.ai.service;

import com.purrbyte.ai.model.dto.Progress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Runs javadoc with the JsonDoclet and handles the process lifecycle.
 *
 * <p>Bundles the javadoc command, executes it, waits for completion with a configurable timeout,
 * and copies the output to the destination directory. Handles timeouts, interruptions, and
 * non-zero exit codes gracefully.
 */
@Slf4j
@Component
public class JavadocRunner {

    private static final int JAVADOC_MAX_DIAGNOSTICS = 100_000;

    private final Path javadocHome;
    private final long javadocTimeoutSeconds;
    private final Path docletDirectory;

    public JavadocRunner(@Value("${doclet.javadoc.home:}") String javadocHome,
                         @Value("${doclet.javadoc.timeout:600}") long javadocTimeoutSeconds,
                         @Value("${doclet.jar.directory:doclet}") Path docletDirectory) {
        this.javadocHome = (javadocHome == null || javadocHome.isBlank())
                ? Path.of(System.getProperty("java.home"))
                : Path.of(javadocHome);
        this.javadocTimeoutSeconds = javadocTimeoutSeconds;
        this.docletDirectory = docletDirectory;
    }

    /**
     * Runs javadoc with the JsonDoclet for the given modules and copies the output to the destination.
     *
     * @param moduleRoot         the extracted source directory
     * @param version            JDK version
     * @param modules            modules to document
     * @param tempOutputDir      temporary directory for Javadoc output
     * @param destinationDir     destination directory for the documentation
     * @param progressCallback   callback for progress updates
     * @param sourceWasExtracted true if the source was newly extracted (allows cleanup)
     * @return path to the generated documentation directory
     */
    public Path run(Path moduleRoot, String version, List<String> modules, Path tempOutputDir, Path destinationDir, Consumer<Progress> progressCallback, boolean sourceWasExtracted) throws IOException {
        if (modules.isEmpty()) {
            throw new IOException("No modules found to document under " + moduleRoot);
        }
        Files.createDirectories(tempOutputDir);
        Path javadocBin = resolveJavadocBin();
        List<String> command = buildCommand(javadocBin, moduleRoot, version, modules, tempOutputDir);
        log.info("Executing javadoc: {}", String.join(" ", command));
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        if (progressCallback != null) {
            progressCallback.accept(Progress.of(0, Progress.MODULE_JAVADOC));
        }
        readProcessOutput(process);
        boolean exited = waitForProcess(process);
        if (!exited) {
            process.destroyForcibly();
            deleteDirectory(tempOutputDir);
            throw new IOException("javadoc process timed out after " + javadocTimeoutSeconds + " seconds");
        }
        int exitCode = process.exitValue();
        validateJavadocOutput(tempOutputDir, exitCode);
        copyDirectory(tempOutputDir, destinationDir);
        if (sourceWasExtracted && Files.exists(moduleRoot)) {
            try {
                deleteDirectory(moduleRoot);
            } catch (IOException e) {
                log.warn("Failed to clean up source dir {}: {}", moduleRoot, e.getMessage());
            }
        }
        if (progressCallback != null) {
            progressCallback.accept(Progress.of(100, Progress.MODULE_JAVADOC));
        }
        log.info("Javadoc completed for version {}", version);
        return destinationDir;
    }

    private Path resolveJavadocBin() throws IOException {
        String osName = System.getProperty("os.name").toLowerCase();
        Path javadocBin = javadocHome.resolve("bin").resolve(osName.contains("win") ? "javadoc.exe" : "javadoc");
        if (!Files.exists(javadocBin)) {
            throw new IOException("javadoc binary not found at: " + javadocBin
                    + " (configure doclet.javadoc.home to a valid JDK home)");
        }
        return javadocBin;
    }

    private List<String> buildCommand(Path javadocBin, Path moduleRoot, String version, List<String> modules, Path tempOutputDir) {
        List<String> command = new ArrayList<>();
        command.add(javadocBin.toString());
        String docletPath = resolveDocletPath();
        if (docletPath != null) {
            command.add("-docletpath");
            command.add(docletPath);
        }
        command.add("-doclet");
        command.add("com.purrbyte.ai.doclet.JsonDoclet");
        command.add("--module-source-path");
        command.add(moduleRoot.toString());
        command.add("--module");
        command.add(String.join(",", modules));
        command.add("-d");
        command.add(tempOutputDir.toString());
        command.add("--pretty");
        command.add("--doc-version");
        command.add(version);
        // The JDK source references build-time-generated symbols that may be absent; raise the
        // diagnostic limits so Javadoc still runs the doclet instead of aborting at the default cap.
        command.add("-Xmaxerrs");
        command.add(String.valueOf(JAVADOC_MAX_DIAGNOSTICS));
        command.add("-Xmaxwarns");
        command.add(String.valueOf(JAVADOC_MAX_DIAGNOSTICS));
        return command;
    }

    private void readProcessOutput(Process process) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[javadoc] {}", line);
            }
        } catch (IOException e) {
            log.warn("Error reading javadoc output: {}", e.getMessage());
        }
    }

    private boolean waitForProcess(Process process) throws IOException {
        try {
            return process.waitFor(javadocTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("javadoc process interrupted", e);
        }
    }

    private void validateJavadocOutput(Path tempOutputDir, int exitCode) throws IOException {
        if (!Files.exists(tempOutputDir.resolve("index.json"))) {
            deleteDirectory(tempOutputDir);
            throw new IOException("javadoc did not produce index.json (exit code " + exitCode + ")");
        }
        if (exitCode != 0) {
            log.warn("javadoc exited with code {} but index.json was produced; continuing with partial documentation", exitCode);
        }
    }

    String resolveDocletPath() {
        try (var stream = Files.list(docletDirectory)) {
            return stream.filter(p -> p.getFileName().toString().equals("JAIDoc-doclet.jar"))
                    .findFirst()
                    .map(path -> {
                        log.debug("Doclet JAR resolved: {}", path);
                        return path.toString();
                    })
                    .orElse(null);
        } catch (IOException e) {
            log.debug("Failed to list doclet directory: {}", e.getMessage());
            return null;
        }
    }

    private void copyDirectory(Path source, Path destination) throws IOException {
        try (var walk = Files.walk(source)) {
            walk.forEach(path -> {
                try {
                    Path dest = destination.resolve(source.relativize(path));
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(dest);
                    } else {
                        Files.copy(path, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            });
        }
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (var walk = Files.walk(dir)) {
            walk.sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            throw new java.io.UncheckedIOException(e);
                        }
                    });
        }
    }
}
