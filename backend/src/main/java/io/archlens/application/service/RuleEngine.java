package io.archlens.application.service;

import io.archlens.application.model.AnalysisDocument;
import io.archlens.application.model.RuleDefinition;
import io.archlens.domain.DimensionKey;
import io.archlens.domain.FindingCertainty;
import io.archlens.domain.FindingOrigin;
import io.archlens.domain.FindingSeverity;
import io.archlens.domain.FindingStatus;
import io.archlens.infrastructure.persistence.FindingEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@ApplicationScoped
public class RuleEngine {

    @Inject
    RuleLoader ruleLoader;

    public List<FindingEntity> evaluate(UUID assessmentId, UUID organizationId, AnalysisDocument doc) {
        List<FindingEntity> findings = new ArrayList<>();
        Instant now = Instant.now();

        for (RuleDefinition rule : ruleLoader.getRules()) {
            if (!rule.enabled()) {
                continue;
            }
            Optional<List<String>> evidence = match(rule, doc);
            if (evidence.isEmpty()) {
                continue;
            }
            FindingEntity f = new FindingEntity();
            f.id = UUID.randomUUID();
            f.organizationId = organizationId;
            f.assessmentId = assessmentId;
            f.ruleId = rule.id();
            f.title = rule.title();
            f.description = rule.description();
            f.severity = rule.severity();
            f.status = FindingStatus.OPEN;
            f.evidenceIds = String.join(",", evidence.get());
            f.dimensionKey = rule.dimension();
            f.origin = FindingOrigin.AUTOMATED;
            f.certainty = rule.certainty() == null ? FindingCertainty.HEURISTIC : rule.certainty();
            f.createdAt = now;
            f.updatedAt = now;
            findings.add(f);
        }
        return findings;
    }

    private Optional<List<String>> match(RuleDefinition rule, AnalysisDocument doc) {
        String type = rule.condition().type();
        Map<String, Object> p = rule.condition().params();
        return switch (type) {
            case "signal_present" -> matchSignal(doc, (String) p.get("signalName"));
            case "framework_present" -> matchFramework(doc, (String) p.get("framework"));
            case "dependency_matches" -> matchDependency(doc, (String) p.get("pattern"));
            case "metric_gt" -> matchMetricGt(doc, p);
            case "cycle_present" -> doc.cycles() != null && !doc.cycles().isEmpty()
                    ? Optional.of(doc.cycles().getFirst().evidenceIds()) : Optional.empty();
            case "secret_present" -> matchSecret(doc, (String) p.get("secretType"));
            case "hotspot_present" -> matchHotspot(doc, (String) p.get("reason"));
            default -> Optional.empty();
        };
    }

    private Optional<List<String>> matchSignal(AnalysisDocument doc, String name) {
        if (doc.signals() == null) return Optional.empty();
        return doc.signals().stream()
                .filter(s -> s.name().toLowerCase().contains(name.toLowerCase())
                        || s.id().toLowerCase().contains(name.toLowerCase()))
                .findFirst()
                .map(AnalysisDocument.Signal::evidenceIds);
    }

    private Optional<List<String>> matchFramework(AnalysisDocument doc, String framework) {
        if (doc.inventory() == null || doc.inventory().endpoints() == null) return Optional.empty();
        return doc.inventory().endpoints().stream()
                .filter(e -> e.type().equalsIgnoreCase(framework)
                        || framework.equals("SERVLET") && "SERVLET".equals(e.type())
                        || framework.equals("JSF") && e.pathOrName().toLowerCase().contains("jsf")
                        || framework.equals("SPRING_MVC") && "SPRING_MVC".equals(e.type()))
                .findFirst()
                .map(e -> List.of(e.evidenceId()));
    }

    private Optional<List<String>> matchDependency(AnalysisDocument doc, String pattern) {
        if (doc.dependencies() == null) return Optional.empty();
        Pattern pat = Pattern.compile(Pattern.quote(pattern), Pattern.CASE_INSENSITIVE);
        return doc.dependencies().stream()
                .filter(d -> pat.matcher(d.coordinates()).find())
                .findFirst()
                .map(d -> List.of("dep:" + d.coordinates()));
    }

    private Optional<List<String>> matchMetricGt(AnalysisDocument doc, Map<String, Object> p) {
        if (doc.metrics() == null) return Optional.empty();
        String metric = (String) p.get("metric");
        int threshold = ((Number) p.get("threshold")).intValue();
        int value = switch (metric) {
            case "lineCount" -> doc.metrics().lineCount();
            case "fileCount" -> doc.metrics().fileCount();
            case "javaFileCount" -> doc.metrics().javaFileCount();
            case "typeCount" -> doc.metrics().typeCount();
            case "testFileCount" -> doc.metrics().testFileCount();
            case "dependencyCount" -> doc.metrics().dependencyCount();
            default -> 0;
        };
        if (p.containsKey("inverseMetric")) {
            String inv = (String) p.get("inverseMetric");
            int invThreshold = ((Number) p.get("inverseThreshold")).intValue();
            int invValue = "testFileCount".equals(inv) ? doc.metrics().testFileCount() : 0;
            if (value > threshold && invValue < invThreshold) {
                return Optional.of(List.of("metric:" + metric));
            }
            return Optional.empty();
        }
        if (value > threshold) {
            return Optional.of(List.of("metric:" + metric));
        }
        return Optional.empty();
    }

    private Optional<List<String>> matchSecret(AnalysisDocument doc, String secretType) {
        if (doc.secrets() == null) return Optional.empty();
        return doc.secrets().stream()
                .filter(s -> s.type().equalsIgnoreCase(secretType))
                .findFirst()
                .map(s -> List.of("secret:" + s.path() + ":" + s.line()));
    }

    private Optional<List<String>> matchHotspot(AnalysisDocument doc, String reason) {
        if (doc.hotspots() == null) return Optional.empty();
        return doc.hotspots().stream()
                .filter(h -> h.reason().equalsIgnoreCase(reason))
                .findFirst()
                .map(h -> List.of(h.evidenceId()));
    }
}
