package io.archlens.analyzer.core.evidence;

import io.archlens.analyzer.core.hash.HashUtil;
import io.archlens.analyzer.core.model.Evidence;

public final class EvidenceFactory {

    private EvidenceFactory() {
    }

    public static Evidence fileEvidence(String id, String relativePath) {
        return new Evidence(id, relativePath, "FILE", relativePath, null);
    }

    public static Evidence lineEvidence(String id, String relativePath, int line, String snippet) {
        String locator = "line:" + line;
        String snippetHash = snippet == null ? null : HashUtil.sha256(snippet.trim());
        return new Evidence(id, relativePath, "LINE_RANGE", locator, snippetHash);
    }

    public static Evidence patternEvidence(String id, String relativePath, String pattern, String snippet) {
        String locator = "pattern:" + pattern;
        String snippetHash = snippet == null ? null : HashUtil.sha256(snippet.trim());
        return new Evidence(id, relativePath, "PATTERN", locator, snippetHash);
    }

    public static Evidence manifestEvidence(String id, String relativePath, String locator) {
        return new Evidence(id, relativePath, "MANIFEST", locator, null);
    }

    public static String evidenceId(String prefix, String relativePath, String suffix) {
        return prefix + ":" + relativePath.replace('\\', '/') + (suffix == null ? "" : ":" + suffix);
    }
}
