package io.archlens.application.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.archlens.api.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Algoritmo {@value #ALGORITHM}, o mesmo do analyzer:
 * chaves ordenadas, nulos omitidos, sem {@code generatedAt}, {@code durationMs} e {@code contentHash}.
 */
public final class CanonicalContentHash {

    public static final String ALGORITHM = "archlens-content-hash/v1";

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .addModule(new JavaTimeModule())
            .build();

    private CanonicalContentHash() {
    }

    public static String compute(String rawJson) {
        try {
            Map<String, Object> payload = MAPPER.readValue(rawJson, new TypeReference<LinkedHashMap<String, Object>>() {});
            payload.remove("generatedAt");
            payload.remove("durationMs");
            payload.remove("contentHash");
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(MAPPER.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new BusinessException("Não foi possível calcular o contentHash canônico");
        }
    }
}
