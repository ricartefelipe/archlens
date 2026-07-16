package io.archlens.analyzer.core.model;

import java.util.List;

public record Signal(String id, String category, String name, String confidence, List<String> evidenceIds) {
}
