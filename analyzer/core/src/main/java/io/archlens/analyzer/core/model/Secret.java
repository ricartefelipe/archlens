package io.archlens.analyzer.core.model;

public record Secret(String type, String path, int line, String valueHash) {
}
