package io.archlens.analyzer.core.detection;

import io.archlens.analyzer.core.evidence.EvidenceFactory;
import io.archlens.analyzer.core.hash.HashUtil;
import io.archlens.analyzer.core.model.Evidence;
import io.archlens.analyzer.core.model.Secret;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detecta potenciais segredos sem persistir o valor. Evidências usam apenas tipo/caminho/linha
 * e marcadores seguros — nunca {@code matcher.group()} com o segredo.
 */
public final class SecretDetector {

    private static final String DETECTOR_ID = "regex.secret-detector";

    private static final Set<String> PLACEHOLDER_VALUES = Set.of(
            "password", "passwd", "secret", "changeme", "changeit", "todo", "fixme",
            "your_password", "your-password", "xxx", "xxxx", "*****", "******",
            "${password}", "${secret}", "${api.key}", "${api_key}", "example", "sample",
            "placeholder", "redacted", "<password>", "null", "none", "n/a"
    );

    private static final Pattern PASSWORD = Pattern.compile(
            "(?i)(password|passwd|pwd)\\s*[=:]\\s*[\"']([^\"']{3,})[\"']"
    );
    private static final Pattern API_KEY = Pattern.compile(
            "(?i)(api[_-]?key|apikey)\\s*[=:]\\s*[\"']([^\"']{8,})[\"']"
    );
    private static final Pattern TOKEN = Pattern.compile(
            "(?i)(access[_-]?token|auth[_-]?token|bearer|token)\\s*[=:]\\s*[\"']([^\"']{8,})[\"']"
    );
    private static final Pattern CONNECTION_STRING = Pattern.compile(
            "(?i)((?:jdbc:[a-z0-9]+://[^\\s\"']+)|(?:(?:mongodb|amqp|redis)://[^\\s\"']+)|(?:ConnectionString\\s*=\\s*[\"']([^\"']+)[\"']))"
    );
    private static final Pattern PRIVATE_KEY = Pattern.compile(
            "-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----"
    );
    private static final Pattern ENV_OR_PLACEHOLDER = Pattern.compile(
            "^\\$\\{[^}]+}$|^%[A-Z0-9_]+%$|^\\{\\{[^}]+}}$|^ENV\\.[A-Z0-9_]+$",
            Pattern.CASE_INSENSITIVE
    );

    private SecretDetector() {
    }

    public static List<SecretFinding> detect(String relativePath, String content) {
        List<SecretFinding> findings = new ArrayList<>();
        scanCapture(relativePath, content, PASSWORD, "PASSWORD", findings);
        scanCapture(relativePath, content, API_KEY, "API_KEY", findings);
        scanCapture(relativePath, content, TOKEN, "TOKEN", findings);
        scanConnectionString(relativePath, content, findings);
        scanPrivateKey(relativePath, content, findings);
        findings.sort((a, b) -> {
            int path = a.secret().path().compareTo(b.secret().path());
            if (path != 0) {
                return path;
            }
            int line = Integer.compare(a.secret().line(), b.secret().line());
            if (line != 0) {
                return line;
            }
            return a.secret().type().compareTo(b.secret().type());
        });
        return findings;
    }

    private static void scanCapture(
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
            if (isBenign(value)) {
                continue;
            }
            int line = lineNumber(content, matcher.start(), lines);
            findings.add(createFinding(type, relativePath, line, value));
        }
    }

    private static void scanConnectionString(String relativePath, String content, List<SecretFinding> findings) {
        String[] lines = content.split("\\R", -1);
        Matcher matcher = CONNECTION_STRING.matcher(content);
        while (matcher.find()) {
            String value = matcher.group(1);
            if (value == null) {
                value = matcher.group(2);
            }
            if (value == null || isBenign(value)) {
                continue;
            }
            String lower = value.toLowerCase(Locale.ROOT);
            if (!(lower.contains("password=") || lower.contains("pwd=") || lower.contains("://"))) {
                continue;
            }
            if (lower.contains("${") || lower.contains("example.com") || lower.contains("localhost")
                    && !lower.contains("password=")) {
                if (!lower.contains("password=") && !lower.contains("pwd=")) {
                    continue;
                }
            }
            int line = lineNumber(content, matcher.start(), lines);
            findings.add(createFinding("CONNECTION_STRING", relativePath, line, "redacted-connection"));
        }
    }

    private static void scanPrivateKey(String relativePath, String content, List<SecretFinding> findings) {
        String[] lines = content.split("\\R", -1);
        Matcher matcher = PRIVATE_KEY.matcher(content);
        while (matcher.find()) {
            int line = lineNumber(content, matcher.start(), lines);
            findings.add(createFinding("PRIVATE_KEY", relativePath, line, "PRIVATE_KEY_MARKER"));
        }
    }

    private static SecretFinding createFinding(String type, String relativePath, int line, String valueMaterial) {
        String evidenceId = EvidenceFactory.evidenceId("secret", relativePath, type + ":" + line);
        // Marcador seguro — nunca o valor nem matcher.group() completo
        Evidence evidence = EvidenceFactory.lineEvidence(evidenceId, relativePath, line, type);
        String indicator = HashUtil.secretOccurrenceIndicator(type, relativePath, line, valueMaterial);
        Secret secret = new Secret(type, relativePath, line, DETECTOR_ID, "HIGH", indicator);
        return new SecretFinding(secret, evidence);
    }

    private static boolean isBenign(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String trimmed = value.trim();
        if (PLACEHOLDER_VALUES.contains(trimmed.toLowerCase(Locale.ROOT))) {
            return true;
        }
        if (ENV_OR_PLACEHOLDER.matcher(trimmed).matches()) {
            return true;
        }
        if (trimmed.startsWith("${") && trimmed.endsWith("}")) {
            return true;
        }
        if (trimmed.equalsIgnoreCase("true") || trimmed.equalsIgnoreCase("false")) {
            return true;
        }
        return false;
    }

    private static int lineNumber(String content, int index, String[] lines) {
        int pos = 0;
        for (int i = 0; i < lines.length; i++) {
            pos += lines[i].length() + 1;
            if (pos > index) {
                return i + 1;
            }
        }
        return Math.max(1, lines.length);
    }

    public record SecretFinding(Secret secret, Evidence evidence) {
    }
}
