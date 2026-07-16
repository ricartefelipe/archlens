package io.archlens.api.dto;

import io.archlens.domain.DimensionKey;
import io.archlens.domain.FindingSeverity;
import io.archlens.domain.FindingStatus;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FindingDtos {
    private FindingDtos() {}

    public record UpdateFindingStatusRequest(
            @NotNull FindingStatus status,
            String reviewNote
    ) {}

    public record FindingResponse(
            UUID id,
            String ruleId,
            String title,
            String description,
            FindingSeverity severity,
            FindingStatus status,
            List<String> evidenceIds,
            DimensionKey dimensionKey,
            String reviewNote,
            String reviewedBy,
            Instant reviewedAt,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
