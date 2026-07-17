package io.archlens.application.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AnalysisDocument(
        String schemaVersion,
        String analyzerVersion,
        String generatedAt,
        Long durationMs,
        String contentHash,
        Project project,
        Inventory inventory,
        List<Signal> signals,
        List<Dependency> dependencies,
        List<Hotspot> hotspots,
        List<Cycle> cycles,
        List<Secret> secrets,
        List<Evidence> evidences,
        Metrics metrics
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Project(String rootName, List<String> buildSystems, List<String> languages, String javaVersionHint) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Inventory(
            List<Module> modules,
            List<PackageInfo> packages,
            List<TypeInfo> types,
            List<Endpoint> endpoints,
            List<Persistence> persistence,
            List<Messaging> messaging
    ) {
        public record Module(String name, String path, String buildSystem) {}
        public record PackageInfo(String name, int fileCount, int lineCount) {}
        public record TypeInfo(String fqn, String kind, String packageName, String path, int lineCount, List<String> annotations) {}
        public record Endpoint(String type, String pathOrName, String sourcePath, String evidenceId) {}
        public record Persistence(String technology, String name, String sourcePath, String evidenceId) {}
        public record Messaging(String technology, String name, String sourcePath, String evidenceId) {}
    }

    public record Signal(String id, String category, String name, String confidence, List<String> evidenceIds) {}
    public record Dependency(String coordinates, String scope, String sourcePath) {}
    public record Hotspot(String path, int lineCount, String reason, String evidenceId) {}
    public record Cycle(List<String> packages, List<String> evidenceIds) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Secret(String type, String path, int line, String detector, String confidence, String valueHash) {}
    public record Evidence(String id, String path, String kind, String locator, String snippetHash) {}
    public record Metrics(int fileCount, int javaFileCount, int lineCount, int typeCount, int testFileCount, int dependencyCount) {}
}
