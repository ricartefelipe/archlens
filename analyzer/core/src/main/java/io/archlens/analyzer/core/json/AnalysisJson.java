package io.archlens.analyzer.core.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.archlens.analyzer.core.hash.HashUtil;
import io.archlens.analyzer.core.model.AnalysisDocument;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AnalysisJson {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .addModule(new JavaTimeModule())
            .build();

    private AnalysisJson() {
    }

    public static String writePretty(AnalysisDocument document) throws JsonProcessingException {
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(document);
    }

    /**
     * Hash canônico do conteúdo estável: exclui {@code generatedAt}, {@code durationMs} e {@code contentHash}.
     */
    public static String computeContentHash(AnalysisDocument document) throws JsonProcessingException {
        Map<String, Object> payload = MAPPER.convertValue(document, Map.class);
        payload.remove("generatedAt");
        payload.remove("durationMs");
        payload.remove("contentHash");
        String canonical = MAPPER.writeValueAsString(payload);
        return HashUtil.sha256(canonical);
    }

    public static AnalysisDocument withHash(AnalysisDocument document) throws JsonProcessingException {
        String hash = computeContentHash(document);
        return new AnalysisDocument(
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
    }

    public static AnalysisDocument read(String json) throws JsonProcessingException {
        return MAPPER.readValue(json, AnalysisDocument.class);
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(AnalysisDocument document) {
        return MAPPER.convertValue(document, LinkedHashMap.class);
    }
}
