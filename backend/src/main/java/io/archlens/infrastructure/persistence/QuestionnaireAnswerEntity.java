package io.archlens.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "questionnaire_answer")
public class QuestionnaireAnswerEntity extends PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    public UUID id;
    @Column(name = "organization_id", columnDefinition = "uuid")
    public UUID organizationId;
    @Column(name = "assessment_id", columnDefinition = "uuid")
    public UUID assessmentId;
    @Column(name = "questionnaire_version")
    public String questionnaireVersion;
    @Lob
    @Column(name = "answers_json")
    public String answersJson;
    @Column(name = "submitted_by")
    public String submittedBy;
    @Column(name = "submitted_at")
    public Instant submittedAt;

    public static QuestionnaireAnswerEntity findByAssessment(UUID assessmentId) {
        return find("assessmentId = ?1", assessmentId).firstResult();
    }
}
