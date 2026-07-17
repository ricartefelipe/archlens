package io.archlens.analyzer.core.evidence;

import io.archlens.analyzer.core.hash.HashUtil;
import io.archlens.analyzer.core.model.Evidence;

public final class EvidenceFactory {

    private EvidenceFactory() {
    }

    public static Evidence fileEvidence(String id, String relativePath) {
        return new Evidence(id, relativePath, "FILE", relativePath, null);
    }

    /**
     * Evidência de linha sem armazenar trecho de código. O {@code marker} deve ser um
     * identificador seguro (ex.: nome do detector), nunca o match bruto nem valor secreto.
     */
    public static Evidence lineEvidence(String id, String relativePath, int line, String marker) {
        String locator = "line:" + line;
        String snippetHash = marker == null || marker.isBlank()
                ? null
                : HashUtil.sha256("archlens/evidence-marker/v1\0" + marker.trim());
        return new Evidence(id, relativePath, "LINE_RANGE", locator, snippetHash);
    }

    public static Evidence patternEvidence(String id, String relativePath, String pattern, String marker) {
        String locator = "pattern:" + pattern;
        String snippetHash = marker == null || marker.isBlank()
                ? null
                : HashUtil.sha256("archlens/evidence-marker/v1\0" + marker.trim());
        return new Evidence(id, relativePath, "PATTERN", locator, snippetHash);
    }

    public static Evidence manifestEvidence(String id, String relativePath, String locator) {
        return new Evidence(id, relativePath, "MANIFEST", locator, null);
    }

    public static String evidenceId(String prefix, String relativePath, String suffix) {
        return prefix + ":" + relativePath.replace('\\', '/') + (suffix == null ? "" : ":" + suffix);
    }
}
