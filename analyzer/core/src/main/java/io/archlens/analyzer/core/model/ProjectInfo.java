package io.archlens.analyzer.core.model;

import java.util.List;

public record ProjectInfo(
        String rootName,
        List<String> buildSystems,
        List<String> languages,
        String javaVersionHint
) {
}
