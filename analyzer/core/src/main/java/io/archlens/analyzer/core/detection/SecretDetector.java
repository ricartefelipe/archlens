package io.archlens.analyzer.core.detection;

import io.archlens.analyzer.core.evidence.EvidenceFactory;
import io.archlens.analyzer.core.hash.HashUtil;
import io.archlens.analyzer.core.model.Evidence;
import io.archlens.analyzer.core.model.Secret;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SecretDetector {

    private static final Pattern PASSWORD = Pattern.compile(
            "(password|passwd|pwd)\\s*=\\s*[\"']([^\"']{3,})[\"']",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern API_KEY = Pattern.compile(
            "(api[_-]?key|apikey)\\s*=\\s*[\"']([^\"']{8,})[\"']",
            Pattern.CASE_INSENSITIVE
    );

    private SecretDetector() {
    }

    public static List<SecretFinding> detect(String relativePath, String content) {
        List<SecretFinding> findings = new ArrayList<>();
        scanPattern(relativePath, content, PASSWORD, "PASSWORD", findings);
        scanPattern(relativePath, content, API_KEY, "API_KEY", findings);
        return findings;
    }

    private static void scanPattern(
            String relativePath,
            String content,
            Pattern pattern,
            String type,
            List<SecretFinding> findings
    ) {
        String[] lines = content.split("\\R", -1);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String value = matcher.group(2);
            int line = lineNumber(content, matcher.start(), lines);
            String evidenceId = EvidenceFactory.evidenceId("secret", relativePath, type + ":" + line);
            Evidence evidence = EvidenceFactory.lineEvidence(evidenceId, relativePath, line, matcher.group());
            findings.add(new SecretFinding(
                    new Secret(type, relativePath, line, HashUtil.sha256(value)),
                    evidence
            ));
        }
    }

    private static int lineNumber(String content, int index, String[] lines) {
        int pos = 0;
        for (int i = 0; i < lines.length; i++) {
            pos += lines[i].length() + 1;
            if (pos > index) {
                return i + 1;
            }
        }
        return lines.length;
    }

    public record SecretFinding(Secret secret, Evidence evidence) {
    }
}
