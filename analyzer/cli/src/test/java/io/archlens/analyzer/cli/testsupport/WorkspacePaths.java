package io.archlens.analyzer.cli.testsupport;

import java.nio.file.Files;
import java.nio.file.Path;

public final class WorkspacePaths {

    private WorkspacePaths() {
    }

    public static Path sampleLegacyDemo() {
        return findRelative("samples/legacy-java-demo");
    }

    public static Path analysisSchema() {
        return findRelative("contracts/analysis.schema.json");
    }

    private static Path findRelative(String relative) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(relative);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate " + relative + " from working directory");
    }
}
