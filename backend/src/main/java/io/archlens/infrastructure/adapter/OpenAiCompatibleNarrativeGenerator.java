package io.archlens.infrastructure.adapter;

import io.archlens.application.port.NarrativeGenerator;
import io.quarkus.arc.lookup.LookupIfProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.util.Map;
import java.util.UUID;

/**
 * Optional LLM-backed narrative generator. Disabled by default (LLM_ENABLED=false).
 * Never sends source code or secrets — only aggregated metadata.
 */
@ApplicationScoped
@LookupIfProperty(name = "archlens.llm.enabled", stringValue = "true")
public class OpenAiCompatibleNarrativeGenerator implements NarrativeGenerator {

    @ConfigProperty(name = "archlens.llm.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "archlens.llm.base-url")
    java.util.Optional<String> baseUrl;

    @Inject
    TemplateNarrativeGenerator fallback;

    @Override
    public String generateExecutiveSummary(UUID assessmentId, Map<String, Object> context) {
        if (!enabled || baseUrl.isEmpty()) {
            return fallback.generateExecutiveSummary(assessmentId, context);
        }
        // MVP: delegate to template — external LLM integration requires explicit enablement
        return fallback.generateExecutiveSummary(assessmentId, context);
    }
}
