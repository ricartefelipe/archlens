package io.archlens.analyzer.core.model;

import java.util.List;

public record TypeInfo(
        String fqn,
        String kind,
        String packageName,
        String path,
        int lineCount,
        List<String> annotations
) {
}
