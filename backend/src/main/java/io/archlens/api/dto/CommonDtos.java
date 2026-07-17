package io.archlens.api.dto;

import io.archlens.domain.DimensionKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CommonDtos {
    private CommonDtos() {}

    public record DimensionScoreResponse(DimensionKey dimensionKey, BigDecimal score, String rationale, Instant updatedAt) {}

    public record QuestionnaireResponse(String version, String title, List<QuestionItem> questions) {
        public record QuestionItem(String id, String text, String type, Integer min, Integer max, String dimension) {}
    }

    public record SubmitQuestionnaireRequest(@NotEmpty Map<String, Integer> answers) {}

    public record ImportResponse(UUID importId, String contentHash, Instant importedAt, int findingsCount) {}

    public record ModernizationOptionRequest(
            @NotBlank String title,
            String description,
            @NotBlank String effort,
            @NotBlank String impact
    ) {}

    public record ModernizationOptionResponse(UUID id, String title, String description, String effort, String impact, Instant createdAt, Instant updatedAt) {}

    public record RoadmapItemRequest(
            @NotBlank String title,
            String description,
            @NotBlank String phase,
            int sortOrder,
            UUID modernizationOptionId
    ) {}

    public record RoadmapItemResponse(UUID id, String title, String description, String phase, int sortOrder, UUID modernizationOptionId, Instant createdAt, Instant updatedAt) {}

    public record ReportSnapshotResponse(UUID id, int version, String contentHash, boolean published, Instant publishedAt, String publishedBy, Instant createdAt) {}

    public record AuditEventResponse(UUID id, UUID assessmentId, String action, String actor, String details, Instant occurredAt) {}
}
