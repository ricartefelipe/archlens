package io.archlens.application.model;

import io.archlens.domain.DimensionKey;
import io.archlens.domain.FindingSeverity;
import java.util.Map;

public record RuleDefinition(
        String id,
        String title,
        String description,
        FindingSeverity severity,
        DimensionKey dimension,
        boolean enabled,
        RuleCondition condition
) {
    public record RuleCondition(String type, Map<String, Object> params) {}
}
