package com.purrbyte.ai.service;

import com.purrbyte.ai.model.dto.Progress;
import com.purrbyte.ai.repository.JdkVersionRepository;
import com.purrbyte.ai.util.JdkDistributionDownloader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Generates JSON documentation for the JDK using the {@code JsonDoclet}.
 *
 * <p>The source is a complete {@code lib/src.zip} (the OpenJDK GitHub source tree is incomplete because
 * many classes — e.g., the {@code java.nio} buffers — are generated at build time). When the requested
 * version matches the running JDK, its local {@code lib/src.zip} is used; otherwise the distribution is
 * downloaded from Adoptium (see {@link JdkDistributionDownloader}) and its {@code lib/src.zip} extracted.
 *
 * <p>Only modular JDKs (11+) are supported. The {@code javadoc} that runs the doclet (configurable via
 * {@code doclet.javadoc.home}, default = the running JDK) must be a JDK 17+ (the doclet needs Jackson 3)
 * whose major version is &gt;= the documented version.
 */
@Slf4j
@Service
public class DocumentationService {

    private static final int MIN_MODULAR_MAJOR = 11;
    private static final int MIN_JAVADOC_MAJOR = 17;

    private final Executor documentationExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final JdkDistributionDownloader jdkDistributionDownloader;
    private final JdkVersionRepository jdkVersionRepository;
    private final SourceExtractor sourceExtractor;
    private final JavadocRunner javadocRunner;
    private final ZipManager zipManager;
    private final Path workDirectory;
    private final Path outputDirectory;
    private final Path javadocHome;
    private final List<String> configuredModules;

    public DocumentationService(
            JdkDistributionDownloader jdkDistributionDownloader,
            JdkVersionRepository jdkVersionRepository,
            SourceExtractor sourceExtractor,
            JavadocRunner javadocRunner,
            ZipManager zipManager,
            @Value("${doclet.work.directory}") Path workDirectory,
            @Value("${data.directory}") Path outputDirectory,
            @Value("${doclet.javadoc.home}") String javadocHome,
            @Value("${doclet.modules}") String modulesCsv) {
        this.jdkDistributionDownloader = jdkDistributionDownloader;
        this.jdkVersionRepository = jdkVersionRepository;
        this.sourceExtractor = sourceExtractor;
        this.javadocRunner = javadocRunner;
        this.zipManager = zipManager;
        this.workDirectory = workDirectory;
        this.outputDirectory = outputDirectory;
        this.javadocHome = (javadocHome == null || javadocHome.isBlank())
                ? Path.of(System.getProperty("java.home"))
                : Path.of(javadocHome);
        this.configuredModules = SourceExtractor.parseModules(modulesCsv);
    }

    /**
     * Generates JDK documentation for the specified version using the JsonDoclet.
     *
     * <p>The pipeline is:
     * <ol>
     *   <li>Obtain a complete {@code lib/src.zip}: the running JDK's own when the version matches,
     *       otherwise download the Adoptium distribution and extract its {@code lib/src.zip}</li>
     *   <li>Extract the source archive — reports {@link Progress#MODULE_EXTRACT}</li>
     *   <li>Run javadoc with the JsonDoclet in module mode and copy to the output directory —
     *       reports {@link Progress#MODULE_JAVADOC}</li>
     * </ol>
     *
     * @param version          JDK version (e.g. "21.0.11", "25.0.3"); must be a modular JDK (11+)
     * @param progressCallback callback for progress updates (each phase reports its own 0-100%)
     * @return future with the path to the generated documentation directory in the output directory
     */
    public CompletableFuture<Path> generateJdkDocumentation(String version, Consumer<Progress> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
                    try {
                        int requestedMajor = JdkDistributionDownloader.extractMajorVersion(version);
                        validateRequest(version, requestedMajor);
                        Consumer<Double> extractCallback = p -> {
                            if (progressCallback != null) {
                                progressCallback.accept(Progress.of(p, Progress.MODULE_EXTRACT));
                            }
                        };
                        Path extractDir = workDirectory.resolve("jdk-sources").resolve(version);
                        boolean sourceWasExtracted;
                        if (Files.exists(extractDir)) {
                            log.info("Source already extracted at {}", extractDir);
                            sourceWasExtracted = false;
                        } else {
                            Path archive = downloadDistribution(version, progressCallback);
                            sourceWasExtracted = sourceExtractor.extractSourceZip(sourceExtractor.extractSrcZipFromArchive(archive, version), version, extractCallback);
                        }
                        List<String> modules = sourceExtractor.resolveModules(extractDir, configuredModules);
                        log.info("Documenting JDK {} ({} modules)", version, modules.size());
                        Path tempOutputDir = workDirectory.resolve("javadoc-out").resolve(version);
                        Path versionDir = outputDirectory.resolve("jdk").resolve(version);
                        javadocRunner.run(extractDir, version, modules, tempOutputDir, versionDir, progressCallback, sourceWasExtracted);
                        zipManager.zipVersion(versionDir, version);
                        return versionDir;
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                }, documentationExecutor)
                .exceptionally(ex -> {
                    log.error("JDK documentation generation failed for version {}: {}", version, ex.getMessage());
                    throw new CompletionException(ex);
                });
    }

    /**
     * Validates that the requested version is documentable: it must be a modular JDK (11+), and the
     * javadoc JDK (configured or running) must be 17+ (the doclet needs Jackson 3) and not older than
     * the requested major (a newer tool reads an older source, not the other way around).
     */
    private void validateRequest(String version, int requestedMajor) throws IOException {
        if (requestedMajor < MIN_MODULAR_MAJOR) {
            throw new IOException("Documenting JDK " + version + " is not supported yet: only modular JDKs ("
                    + MIN_MODULAR_MAJOR + "+) are handled. JDK 8 support is planned.");
        }
        int javadocMajor = javadocJdkMajor();
        if (javadocMajor >= 0 && javadocMajor < MIN_JAVADOC_MAJOR) {
            throw new IOException("The javadoc JDK (" + javadocHome + ", major " + javadocMajor + ") must be "
                    + MIN_JAVADOC_MAJOR + " or newer; the doclet requires Jackson 3 (JDK " + MIN_JAVADOC_MAJOR + "+).");
        }
        if (javadocMajor >= 0 && javadocMajor < requestedMajor) {
            throw new IOException("The javadoc JDK (major " + javadocMajor + ") cannot document newer JDK "
                    + version + " source. Configure doclet.javadoc.home with a JDK whose major is >= " + requestedMajor + ".");
        }
    }

    /**
     * Determines the major version of the javadoc JDK: the runtime feature version when it is the
     * running JDK, otherwise parsed from the JDK home's {@code release} file. Returns {@code -1} when
     * it cannot be determined (validation is then skipped).
     */
    private int javadocJdkMajor() {
        if (javadocHome.equals(Path.of(System.getProperty("java.home")))) {
            return Runtime.version().feature();
        }
        Path release = javadocHome.resolve("release");
        if (Files.exists(release)) {
            try {
                for (String line : Files.readAllLines(release)) {
                    if (line.startsWith("JAVA_VERSION=")) {
                        String value = line.substring("JAVA_VERSION=".length()).replace("\"", "").trim();
                        return JdkDistributionDownloader.extractMajorVersion(value);
                    }
                }
            } catch (Exception e) {
                log.debug("Could not read JDK version from {}: {}", release, e.getMessage());
            }
        }
        return -1;
    }

    /**
     * Downloads the Adoptium JDK distribution for the version, unwrapping the async failure.
     */
    private Path downloadDistribution(String version, Consumer<Progress> progressCallback) throws IOException {
        try {
            return jdkDistributionDownloader.downloadDistribution(version, progressCallback).join();
        } catch (CompletionException ce) {
            Throwable cause = ce.getCause();
            if (cause instanceof IOException io) {
                throw io;
            }
            throw new IOException("JDK distribution download failed for " + version, cause);
        }
    }

    /**
     * Lists available JDK versions that have been generated and ingested.
     *
     * <p>Reads from the database (only versions with READY ingest status) instead of
     * scanning the filesystem, which is faster and more reliable.
     *
     * @return list of version strings ordered from the newest major to the oldest
     */
    public List<String> listAvailableVersions() {
        return jdkVersionRepository.findAllVersionStringsOrderByMajorDesc();
    }

    /**
     * Returns the path for a specific version's documentation ZIP, or null if not found.
     * Searches recursively under the configured data directory.
     *
     * @param version JDK version
     * @return path to the version ZIP file, or null
     */
    public Path getVersionZip(String version) {
        return zipManager.findVersionZip(version);
    }

    /**
     * Checks if documentation has been generated for the specified version.
     * Searches recursively under the configured data directory.
     *
     * @param version JDK version
     * @return true if <version>.zip exists and contains index.json
     */
    public boolean isVersionGenerated(String version) {
        return zipManager.isVersionGenerated(version);
    }
}
