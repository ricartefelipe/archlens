package io.archlens.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.archlens.api.dto.CommonDtos.*;
import io.archlens.domain.AuditAction;
import io.archlens.infrastructure.persistence.*;
import io.archlens.infrastructure.security.IdentityService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;

@ApplicationScoped
public class QuestionnaireService {

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @Inject
    DimensionScoreService dimensionScoreService;
    @Inject
    OrganizationService organizationService;
    @Inject
    AuditService auditService;
    @Inject
    IdentityService identityService;
    @Inject
    ObjectMapper objectMapper;

    public QuestionnaireResponse getQuestionnaire() {
        Map<String, Object> data = loadYaml();
        String version = (String) data.get("version");
        String title = (String) data.get("title");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) data.get("questions");
        List<QuestionnaireResponse.QuestionItem> items = questions.stream()
                .map(q -> new QuestionnaireResponse.QuestionItem(
                        (String) q.get("id"),
                        (String) q.get("text"),
                        (String) q.get("type"),
                        (Integer) q.get("min"),
                        (Integer) q.get("max"),
                        (String) q.get("dimension")
                )).toList();
        return new QuestionnaireResponse(version, title, items);
    }

    @Transactional
    public void submit(UUID assessmentId, SubmitQuestionnaireRequest req) {
        identityService.requireWrite();
        var org = organizationService.getOrganization();
        var actor = identityService.current().subject();

        if (AssessmentEntity.findByOrgAndId(org.id, assessmentId) == null) {
            throw new NotFoundException("Avaliação não encontrada");
        }

        Map<String, Object> data = loadYaml();
        String version = (String) data.get("version");

        QuestionnaireAnswerEntity existing = QuestionnaireAnswerEntity.findByAssessment(assessmentId);
        try {
            String json = objectMapper.writeValueAsString(req.answers());
            if (existing == null) {
                existing = new QuestionnaireAnswerEntity();
                existing.id = UUID.randomUUID();
                existing.organizationId = org.id;
                existing.assessmentId = assessmentId;
            }
            existing.questionnaireVersion = version;
            existing.answersJson = json;
            existing.submittedBy = actor;
            existing.submittedAt = Instant.now();
            existing.persist();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao salvar respostas", e);
        }

        dimensionScoreService.applyQuestionnaire(assessmentId, org.id, req.answers());
        auditService.record(org.id, assessmentId, AuditAction.QUESTIONNAIRE_SUBMITTED, actor, "Questionário enviado");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYaml() {
        try (InputStream in = getClass().getResourceAsStream("/questionnaire-v1.yaml")) {
            return yamlMapper.readValue(in, Map.class);
        } catch (Exception e) {
            throw new IllegalStateException("Questionário não encontrado", e);
        }
    }
}
