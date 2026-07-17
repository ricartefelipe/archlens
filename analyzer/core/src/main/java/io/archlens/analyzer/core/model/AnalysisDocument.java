package io.archlens.analyzer.core.model;

import java.util.List;

public record AnalysisDocument(
        String schemaVersion,
        String analyzerVersion,
        String generatedAt,
        Long durationMs,
        String contentHash,
        ProjectInfo project,
        Inventory inventory,
        List<Signal> signals,
        List<Dependency> dependencies,
        List<Hotspot> hotspots,
        List<Cycle> cycles,
        List<Secret> secrets,
        List<Evidence> evidences,
        Metrics metrics
) {
}
