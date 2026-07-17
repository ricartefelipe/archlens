package io.archlens.infrastructure.adapter;

import io.archlens.application.port.NarrativeGenerator;
import io.archlens.infrastructure.persistence.AssessmentEntity;
import io.archlens.infrastructure.persistence.DimensionScoreEntity;
import io.archlens.infrastructure.persistence.FindingEntity;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.text.StringSubstitutor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
@DefaultBean
public class TemplateNarrativeGenerator implements NarrativeGenerator {

    private static final String TEMPLATE = """
            Esta avaliação arquitetural (${projectName}) identificou ${findingCount} achados relevantes.
            A dimensão com maior atenção é ${topDimension} (score ${topScore}).
            Recomenda-se priorizar itens de severidade alta e validar o roadmap proposto com stakeholders.
            """;

    @Override
    public String generateExecutiveSummary(UUID assessmentId, Map<String, Object> context) {
        UUID orgId = null;
        if (context != null && context.get("organizationId") != null) {
            orgId = UUID.fromString(String.valueOf(context.get("organizationId")));
        }

        AssessmentEntity assessment;
        List<FindingEntity> findings;
        List<DimensionScoreEntity> dimensions;
        if (orgId != null) {
            assessment = AssessmentEntity.findByOrgAndId(orgId, assessmentId);
            findings = FindingEntity.listByOrgAndAssessment(orgId, assessmentId);
            dimensions = DimensionScoreEntity.listByOrgAndAssessment(orgId, assessmentId);
        } else {
            assessment = AssessmentEntity.findById(assessmentId);
            findings = FindingEntity.listByAssessment(assessmentId);
            dimensions = DimensionScoreEntity.listByAssessment(assessmentId);
        }

        String topDim = "N/A";
        String topScore = "0";
        if (!dimensions.isEmpty()) {
            var top = dimensions.stream().max((a, b) -> a.score.compareTo(b.score)).orElseThrow();
            topDim = top.dimensionKey.name();
            topScore = top.score.toPlainString();
        }

        Map<String, String> values = new HashMap<>();
        values.put("projectName", assessment != null && assessment.projectRootName != null
                ? assessment.projectRootName : "projeto");
        values.put("findingCount", String.valueOf(findings.size()));
        values.put("topDimension", topDim);
        values.put("topScore", topScore);
        if (context != null) {
            context.forEach((k, v) -> {
                if (!"organizationId".equals(k)) {
                    values.put(k, String.valueOf(v));
                }
            });
        }
        return StringSubstitutor.replace(TEMPLATE, values).trim();
    }
}
