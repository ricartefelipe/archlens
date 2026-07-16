package io.archlens.api.dto;

import io.archlens.domain.AssessmentStatus;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;

public final class AssessmentDtos {
    private AssessmentDtos() {}

    public record CreateAssessmentRequest(@NotBlank String name, String description) {}

    public record UpdateAssessmentRequest(String name, String description, AssessmentStatus status) {}

    public record AssessmentResponse(
            UUID id,
            String name,
            String description,
            AssessmentStatus status,
            String projectRootName,
            String createdBy,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
