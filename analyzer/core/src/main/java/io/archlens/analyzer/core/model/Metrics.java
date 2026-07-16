package io.archlens.analyzer.core.model;

public record Metrics(
        int fileCount,
        int javaFileCount,
        int lineCount,
        int typeCount,
        int testFileCount,
        int dependencyCount
) {
}
