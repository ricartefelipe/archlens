package io.archlens.analyzer.core.model;

import java.util.List;

public record Cycle(List<String> packages, List<String> evidenceIds) {
}
