package io.archlens.application.port;

import java.util.Map;
import java.util.UUID;

public interface NarrativeGenerator {
    String generateExecutiveSummary(UUID assessmentId, Map<String, Object> context);
}
