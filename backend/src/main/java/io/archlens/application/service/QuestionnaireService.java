package io.archlens.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.archlens.api.dto.CommonDtos.*;
import io.archlens.api.exception.BusinessException;
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

/**
 * Questionário versionado. Respostas parciais são aceitas (perguntas omitidas não alteram o fator);
 * reenvio sobrescreve a versão corrente (idempotente por assessment).
 */
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
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) data.get("questions");
        Map<String, Map<String, Object>> byId = new HashMap<>();
        for (Map<String, Object> q : questions) {
            byId.put((String) q.get("id"), q);
        }

        if (req.answers() == null || req.answers().isEmpty()) {
            throw new BusinessException("Informe ao menos uma resposta");
        }

        for (Map.Entry<String, Integer> entry : req.answers().entrySet()) {
            Map<String, Object> q = byId.get(entry.getKey());
            if (q == null) {
                throw new BusinessException("Pergunta desconhecida: " + entry.getKey());
            }
            Integer min = (Integer) q.get("min");
            Integer max = (Integer) q.get("max");
            Integer value = entry.getValue();
            if (value == null || value < min || value > max) {
                throw new BusinessException(
                        "Resposta fora do intervalo [" + min + "," + max + "] para " + entry.getKey());
            }
        }

        QuestionnaireAnswerEntity existing = QuestionnaireAnswerEntity.findByOrgAndAssessment(org.id, assessmentId);
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
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao salvar respostas", e);
        }

        dimensionScoreService.applyQuestionnaire(assessmentId, org.id, req.answers());
        auditService.record(org.id, assessmentId, AuditAction.QUESTIONNAIRE_SUBMITTED, actor,
                "Questionário v" + version + " enviado (" + req.answers().size() + " respostas)");
    }

    public List<DimensionScoreResponse> listDimensions(UUID assessmentId) {
        var org = organizationService.getOrganization();
        if (AssessmentEntity.findByOrgAndId(org.id, assessmentId) == null) {
            throw new NotFoundException("Avaliação não encontrada");
        }
        return DimensionScoreEntity.listByOrgAndAssessment(org.id, assessmentId).stream()
                .map(d -> new DimensionScoreResponse(d.dimensionKey, d.score, d.rationale, d.updatedAt))
                .toList();
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
