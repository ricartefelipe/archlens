package io.archlens.analyzer.core.detection;

import io.archlens.analyzer.core.hash.HashUtil;
import io.archlens.analyzer.core.json.AnalysisJson;
import io.archlens.analyzer.core.model.Secret;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretDetectorTest {

    private static final String FICTIONAL = "demo-not-a-real-secret-123";

    @Test
    void detectsPasswordWithoutPersistingValue() throws Exception {
        String content = """
                public class Cfg {
                  private static final String password = "%s";
                  private static final String apiKey = "ABCD1234EFGH5678";
                  private static final String token = "tok_live_abcdef123456";
                  // password = "${PASSWORD}"
                  // apiKey = "changeme"
                }
                """.formatted(FICTIONAL);

        List<SecretDetector.SecretFinding> findings = SecretDetector.detect("Cfg.java", content);
        assertFalse(findings.isEmpty());
        String json = AnalysisJson.mapper().writeValueAsString(findings);
        assertFalse(json.contains(FICTIONAL));
        assertFalse(json.contains("ABCD1234EFGH5678"));
        assertFalse(json.contains("tok_live_abcdef123456"));

        for (SecretDetector.SecretFinding finding : findings) {
            Secret secret = finding.secret();
            assertTrue(secret.valueHash().startsWith("sha256:"));
            assertEquals("regex.secret-detector", secret.detector());
            assertFalse(finding.evidence().locator().contains(FICTIONAL));
            assertFalse(String.valueOf(finding.evidence().snippetHash()).contains(FICTIONAL));
        }
    }

    @Test
    void ignoresPlaceholdersAndEnvVars() {
        String content = """
                password = "${DB_PASSWORD}"
                apiKey = "changeme"
                token = "ENV.API_TOKEN"
                """;
        assertTrue(SecretDetector.detect("app.properties", content).isEmpty());
    }

    @Test
    void detectsPrivateKeyMarker() {
        String content = """
                -----BEGIN PRIVATE KEY-----
                MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC
                -----END PRIVATE KEY-----
                """;
        List<SecretDetector.SecretFinding> findings = SecretDetector.detect("key.pem", content);
        assertEquals(1, findings.size());
        assertEquals("PRIVATE_KEY", findings.getFirst().secret().type());
        assertFalse(AnalysisJson.mapper().valueToTree(findings).toString().contains("MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC"));
    }

    @Test
    void secretIndicatorIsNotBareValueHash() {
        String bare = HashUtil.sha256(FICTIONAL);
        String indicator = HashUtil.secretOccurrenceIndicator("PASSWORD", "Cfg.java", 2, FICTIONAL);
        assertTrue(indicator.startsWith("sha256:"));
        assertFalse(indicator.equals(bare));
    }
}
