package io.archlens.application.service;

import io.archlens.application.model.AnalysisDocument;
import io.archlens.domain.DimensionKey;
import io.archlens.domain.FindingSeverity;
import io.archlens.infrastructure.persistence.DimensionScoreEntity;
import io.archlens.infrastructure.persistence.FindingEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class DimensionScoreService {

    private static final Map<DimensionKey, BigDecimal> BASE_SCORES = Map.of(
            DimensionKey.BUSINESS_CRITICALITY, BigDecimal.valueOf(50),
            DimensionKey.TEAM_SKILL, BigDecimal.valueOf(50),
            DimensionKey.RISK_TOLERANCE, BigDecimal.valueOf(50),
            DimensionKey.TECHNICAL_DEBT, BigDecimal.valueOf(50),
            DimensionKey.MODERNIZATION_READINESS, BigDecimal.valueOf(50),
            DimensionKey.SECURITY_POSTURE, BigDecimal.valueOf(50),
            DimensionKey.OPERATIONAL_COMPLEXITY, BigDecimal.valueOf(50)
    );

    @ConfigProperty(name = "archlens.scores.disclaimer",
            defaultValue = "Scores apoiam decisão; não são precisão científica.")
    String disclaimer;

    @Transactional
    public void recalculateFromAnalysis(UUID assessmentId, UUID orgId, AnalysisDocument doc, List<FindingEntity> findings) {
        Map<DimensionKey, BigDecimal> scores = new EnumMap<>(BASE_SCORES);
        StringBuilder factors = new StringBuilder("Fatores técnicos: ");

        for (FindingEntity f : findings) {
            if (f.dimensionKey == null) continue;
            BigDecimal penalty = severityPenalty(f.severity);
            scores.merge(f.dimensionKey, penalty, BigDecimal::add);
            factors.append(f.ruleId).append("(").append(f.severity).append(") ");
        }

        if (doc.metrics() != null && doc.metrics().lineCount() > 30000) {
            scores.merge(DimensionKey.OPERATIONAL_COMPLEXITY, BigDecimal.valueOf(10), BigDecimal::add);
            factors.append("lineCount>30000 ");
        }

        persistScores(assessmentId, orgId, scores, factors + "| " + disclaimer);
    }

    @Transactional
    public void applyQuestionnaire(UUID assessmentId, UUID orgId, Map<String, Integer> answers) {
        Map<DimensionKey, BigDecimal> scores = new EnumMap<>(BASE_SCORES);
        List<FindingEntity> findings = FindingEntity.listByOrgAndAssessment(orgId, assessmentId);
        for (FindingEntity f : findings) {
            if (f.dimensionKey == null) continue;
            scores.merge(f.dimensionKey, severityPenalty(f.severity), BigDecimal::add);
        }

        applyAnswer(scores, DimensionKey.BUSINESS_CRITICALITY, answers.get("business_criticality"), 8);
        applyAnswer(scores, DimensionKey.TEAM_SKILL, answers.get("team_skill"), -6);
        applyAnswer(scores, DimensionKey.RISK_TOLERANCE, answers.get("risk_tolerance"), -5);
        applyAnswer(scores, DimensionKey.MODERNIZATION_READINESS, answers.get("change_urgency"), 7);
        applyAnswer(scores, DimensionKey.SECURITY_POSTURE, answers.get("security_priority"), 6);

        String rationale = "Ajustado pelo questionário (respostas parciais permitidas). " + disclaimer;
        persistScores(assessmentId, orgId, scores, rationale);
    }

    private void applyAnswer(Map<DimensionKey, BigDecimal> scores, DimensionKey key, Integer value, int factor) {
        if (value == null) return;
        BigDecimal delta = BigDecimal.valueOf((value - 3) * factor);
        scores.merge(key, delta, BigDecimal::add);
    }

    private BigDecimal severityPenalty(FindingSeverity severity) {
        return switch (severity) {
            case CRITICAL -> BigDecimal.valueOf(20);
            case HIGH -> BigDecimal.valueOf(12);
            case MEDIUM -> BigDecimal.valueOf(6);
            case LOW -> BigDecimal.valueOf(3);
            case INFO -> BigDecimal.valueOf(1);
        };
    }

    private void persistScores(UUID assessmentId, UUID orgId, Map<DimensionKey, BigDecimal> scores, String rationale) {
        Instant now = Instant.now();
        for (DimensionKey key : DimensionKey.values()) {
            BigDecimal raw = scores.getOrDefault(key, BASE_SCORES.get(key));
            BigDecimal clamped = raw.min(BigDecimal.valueOf(100)).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            DimensionScoreEntity existing = DimensionScoreEntity
                    .find("organizationId = ?1 and assessmentId = ?2 and dimensionKey = ?3", orgId, assessmentId, key)
                    .firstResult();

            if (existing == null) {
                existing = new DimensionScoreEntity();
                existing.id = UUID.randomUUID();
                existing.organizationId = orgId;
                existing.assessmentId = assessmentId;
                existing.dimensionKey = key;
            }
            existing.score = clamped;
            existing.rationale = rationale;
            existing.updatedAt = now;
            existing.persist();
        }
    }
}
