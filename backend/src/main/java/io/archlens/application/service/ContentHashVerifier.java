package io.archlens.application.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.archlens.application.model.AnalysisDocument;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;

/**
 * Recalcula o contentHash canônico (mesma regra do CLI) e recusa payload adulterado.
 * A única schemaVersion aceita é {@link #SUPPORTED_SCHEMA}. Não há migração silenciosa.
 */
public final class ContentHashVerifier {

    public static final String SUPPORTED_SCHEMA = "1.0.0";
    private static final Set<String> SUPPORTED = Set.of(SUPPORTED_SCHEMA);

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .addModule(new JavaTimeModule())
            .build();

    private ContentHashVerifier() {
    }

    public static boolean isSupportedSchema(String schemaVersion) {
        return schemaVersion != null && SUPPORTED.contains(schemaVersion);
    }

    public static boolean matches(AnalysisDocument document) {
        if (document == null || document.contentHash() == null) {
            return false;
        }
        try {
            return compute(document).equals(document.contentHash());
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    public static String compute(AnalysisDocument document) throws JsonProcessingException {
        Map<String, Object> payload = MAPPER.convertValue(document, Map.class);
        payload.remove("generatedAt");
        payload.remove("durationMs");
        payload.remove("contentHash");
        String canonical = MAPPER.writeValueAsString(payload);
        return sha256(canonical);
    }

    public static String writeWithHash(AnalysisDocument document) throws JsonProcessingException {
        String hash = compute(document);
        AnalysisDocument stamped = new AnalysisDocument(
                document.schemaVersion(),
                document.analyzerVersion(),
                document.generatedAt(),
                document.durationMs(),
                hash,
                document.project(),
                document.inventory(),
                document.signals(),
                document.dependencies(),
                document.hotspots(),
                document.cycles(),
                document.secrets(),
                document.evidences(),
                document.metrics()
        );
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(stamped);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String hex = HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
            return "sha256:" + hex;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
