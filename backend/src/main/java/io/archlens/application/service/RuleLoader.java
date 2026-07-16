package io.archlens.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.archlens.application.model.RuleDefinition;
import io.archlens.domain.DimensionKey;
import io.archlens.domain.FindingSeverity;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class RuleLoader {

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private List<RuleDefinition> rules = List.of();

    void onStart(@Observes StartupEvent event) {
        this.rules = loadAll();
    }

    public List<RuleDefinition> getRules() {
        return rules;
    }

    public int count() {
        return rules.size();
    }

    @SuppressWarnings("unchecked")
    private List<RuleDefinition> loadAll() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream index = classLoader.getResourceAsStream("rules/rules-index.txt")) {
            if (index == null) {
                throw new IllegalStateException("Índice de regras não encontrado");
            }
            List<RuleDefinition> loaded = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(index, StandardCharsets.UTF_8))) {
                for (String resource : reader.lines().filter(line -> !line.isBlank()).sorted().toList()) {
                    try (InputStream rule = classLoader.getResourceAsStream("rules/" + resource)) {
                        if (rule == null) {
                            throw new IllegalStateException("Regra não encontrada: " + resource);
                        }
                        loaded.add(parseStream(rule));
                    }
                }
            }
            return Collections.unmodifiableList(loaded);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao carregar regras YAML", e);
        }
    }

    @SuppressWarnings("unchecked")
    private RuleDefinition parseStream(InputStream in) throws Exception {
        Map<String, Object> map = yamlMapper.readValue(in, Map.class);
        Map<String, Object> cond = (Map<String, Object>) map.get("condition");
        String type = (String) cond.get("type");
        Map<String, Object> params = new LinkedHashMap<>(cond);
        params.remove("type");
        return new RuleDefinition(
                (String) map.get("id"),
                (String) map.get("title"),
                (String) map.get("description"),
                FindingSeverity.valueOf((String) map.get("severity")),
                DimensionKey.valueOf((String) map.get("dimension")),
                map.get("enabled") == null || Boolean.TRUE.equals(map.get("enabled")),
                new RuleDefinition.RuleCondition(type, params)
        );
    }
}
