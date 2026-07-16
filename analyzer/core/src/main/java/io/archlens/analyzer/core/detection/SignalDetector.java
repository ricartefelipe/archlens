package io.archlens.analyzer.core.detection;

import io.archlens.analyzer.core.model.Dependency;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SignalDetector {

    private static final Map<String, SignalRule> RULES = buildRules();

    private SignalDetector() {
    }

    public static List<DetectedSignal> detectFromDependencies(List<Dependency> dependencies) {
        List<DetectedSignal> signals = new ArrayList<>();
        for (Dependency dependency : dependencies) {
            String coordinates = dependency.coordinates().toLowerCase(Locale.ROOT);
            for (Map.Entry<String, SignalRule> entry : RULES.entrySet()) {
                if (coordinates.contains(entry.getKey())) {
                    signals.add(new DetectedSignal(
                            entry.getValue().id(),
                            entry.getValue().category(),
                            entry.getValue().name(),
                            entry.getValue().confidence(),
                            "dep:" + dependency.coordinates()
                    ));
                }
            }
        }
        return signals;
    }

    public static List<DetectedSignal> detectFromSource(
            String relativePath,
            String content,
            List<String> imports,
            List<String> annotations
    ) {
        List<DetectedSignal> signals = new ArrayList<>();
        String lower = content.toLowerCase(Locale.ROOT);

        if (content.contains("System.out")) {
            signals.add(new DetectedSignal(
                    "quality.system-out",
                    "QUALITY",
                    "System.out usage",
                    "HIGH",
                    "src:" + relativePath + ":system.out"
            ));
        }

        if (hasAnyImport(imports, "javax.")) {
            signals.add(new DetectedSignal(
                    "ee.javax-namespace",
                    "EE",
                    "javax.* namespace",
                    "HIGH",
                    "src:" + relativePath + ":javax"
            ));
        }

        if (containsAny(lower, "@stateless", "@stateful", "@singleton") || hasImport(imports, "javax.ejb")) {
            signals.add(new DetectedSignal(
                    "ee.ejb",
                    "EE",
                    "EJB",
                    "HIGH",
                    "src:" + relativePath + ":ejb"
            ));
        }

        if (containsAny(lower, "@path", "@get", "@post") || hasImport(imports, "javax.ws.rs")) {
            signals.add(new DetectedSignal(
                    "api.jax-rs",
                    "API",
                    "JAX-RS",
                    "HIGH",
                    "src:" + relativePath + ":jaxrs"
            ));
        }

        if (annotations.contains("Entity") || hasImport(imports, "javax.persistence")) {
            signals.add(new DetectedSignal(
                    "persistence.jpa",
                    "PERSISTENCE",
                    "JPA Entity",
                    "HIGH",
                    "src:" + relativePath + ":jpa"
            ));
        }

        if (containsAny(lower, "@webservice", "javax.jws") || hasImport(imports, "javax.jws")) {
            signals.add(new DetectedSignal(
                    "api.soap",
                    "API",
                    "SOAP WebService",
                    "HIGH",
                    "src:" + relativePath + ":soap"
            ));
        }

        if (hasImport(imports, "javax.jms") || lower.contains("@jmsdestination")) {
            signals.add(new DetectedSignal(
                    "messaging.jms",
                    "MESSAGING",
                    "JMS",
                    "HIGH",
                    "src:" + relativePath + ":jms"
            ));
        }

        if (hasImport(imports, "javax.servlet") || lower.contains("extends httpservlet")) {
            signals.add(new DetectedSignal(
                    "api.servlet",
                    "API",
                    "Servlet",
                    "HIGH",
                    "src:" + relativePath + ":servlet"
            ));
        }

        if (hasImport(imports, "javax.faces") || lower.contains("@managedbean")) {
            signals.add(new DetectedSignal(
                    "ui.jsf",
                    "UI",
                    "JSF",
                    "MEDIUM",
                    "src:" + relativePath + ":jsf"
            ));
        }

        if (hasImport(imports, "javax.enterprise") || lower.contains("@inject")) {
            signals.add(new DetectedSignal(
                    "ee.cdi",
                    "EE",
                    "CDI",
                    "MEDIUM",
                    "src:" + relativePath + ":cdi"
            ));
        }

        if (hasImport(imports, "org.springframework")) {
            signals.add(new DetectedSignal(
                    "framework.spring",
                    "FRAMEWORK",
                    "Spring",
                    "HIGH",
                    "src:" + relativePath + ":spring"
            ));
        }

        if (hasImport(imports, "io.quarkus")) {
            signals.add(new DetectedSignal(
                    "framework.quarkus",
                    "FRAMEWORK",
                    "Quarkus",
                    "HIGH",
                    "src:" + relativePath + ":quarkus"
            ));
        }

        if (hasImport(imports, "javax.enterprise") || hasImport(imports, "jakarta.enterprise")) {
            signals.add(new DetectedSignal(
                    "ee.platform",
                    "EE",
                    "Java EE / Jakarta EE",
                    "MEDIUM",
                    "src:" + relativePath + ":ee"
            ));
        }

        if (hasImport(imports, "org.apache.kafka")) {
            signals.add(new DetectedSignal(
                    "messaging.kafka",
                    "MESSAGING",
                    "Kafka",
                    "HIGH",
                    "src:" + relativePath + ":kafka"
            ));
        }

        return signals;
    }

    private static boolean containsAny(String content, String... needles) {
        for (String needle : needles) {
            if (content.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasImport(List<String> imports, String prefix) {
        for (String imp : imports) {
            if (imp.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnyImport(List<String> imports, String prefix) {
        return hasImport(imports, prefix);
    }

    private static Map<String, SignalRule> buildRules() {
        Map<String, SignalRule> rules = new LinkedHashMap<>();
        rules.put("spring-boot", new SignalRule("framework.spring", "FRAMEWORK", "Spring Boot", "HIGH"));
        rules.put("springframework", new SignalRule("framework.spring", "FRAMEWORK", "Spring", "HIGH"));
        rules.put("quarkus", new SignalRule("framework.quarkus", "FRAMEWORK", "Quarkus", "HIGH"));
        rules.put("javax.ejb", new SignalRule("ee.ejb", "EE", "EJB", "HIGH"));
        rules.put("javax.persistence", new SignalRule("persistence.jpa", "PERSISTENCE", "JPA", "HIGH"));
        rules.put("jakarta.persistence", new SignalRule("persistence.jpa", "PERSISTENCE", "JPA", "HIGH"));
        rules.put("javax.ws.rs", new SignalRule("api.jax-rs", "API", "JAX-RS", "HIGH"));
        rules.put("jakarta.ws.rs", new SignalRule("api.jax-rs", "API", "JAX-RS", "HIGH"));
        rules.put("javax.servlet", new SignalRule("api.servlet", "API", "Servlet", "HIGH"));
        rules.put("javax.faces", new SignalRule("ui.jsf", "UI", "JSF", "MEDIUM"));
        rules.put("javax.jms", new SignalRule("messaging.jms", "MESSAGING", "JMS", "HIGH"));
        rules.put("kafka-clients", new SignalRule("messaging.kafka", "MESSAGING", "Kafka", "HIGH"));
        rules.put("cxf", new SignalRule("api.soap", "API", "SOAP", "MEDIUM"));
        rules.put("jaxws", new SignalRule("api.soap", "API", "SOAP", "MEDIUM"));
        return rules;
    }

    private record SignalRule(String id, String category, String name, String confidence) {
    }

    public record DetectedSignal(String id, String category, String name, String confidence, String evidenceKey) {
    }
}
