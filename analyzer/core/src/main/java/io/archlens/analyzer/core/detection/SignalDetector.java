package io.archlens.analyzer.core.detection;

import io.archlens.analyzer.core.model.Dependency;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Detecção heurística por regex/imports/coordenadas — não usa AST.
 * Cada sinal expõe id, categoria, nome, confiança e chave de evidência.
 */
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
                            "dep:" + dependency.coordinates(),
                            "dependency-coordinate"
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

        add(signals, relativePath, "quality.system-out", "QUALITY", "System.out usage", "HIGH",
                content.contains("System.out"), "system.out");
        add(signals, relativePath, "quality.raw-thread", "QUALITY", "Raw Thread usage", "MEDIUM",
                content.contains("new Thread(") || content.contains("extends Thread"), "thread");
        add(signals, relativePath, "quality.rmi", "QUALITY", "RMI usage", "MEDIUM",
                hasAnyImport(imports, "java.rmi") || lower.contains("unicastremoteobject"), "rmi");
        add(signals, relativePath, "quality.native-serialization", "QUALITY", "Java native serialization", "MEDIUM",
                content.contains("implements Serializable") || hasAnyImport(imports, "java.io.ObjectInputStream"),
                "serialization");

        add(signals, relativePath, "ee.javax-namespace", "EE", "javax.* namespace", "HIGH",
                hasAnyImport(imports, "javax."), "javax");
        add(signals, relativePath, "ee.jakarta-namespace", "EE", "jakarta.* namespace", "HIGH",
                hasAnyImport(imports, "jakarta."), "jakarta");
        add(signals, relativePath, "ee.ejb", "EE", "EJB", "HIGH",
                containsAny(lower, "@stateless", "@stateful", "@singleton", "@messageDriven")
                        || hasAnyImport(imports, "javax.ejb", "jakarta.ejb"), "ejb");
        add(signals, relativePath, "ee.cdi", "EE", "CDI", "MEDIUM",
                containsAny(lower, "@inject", "@named", "@applicationScoped", "@requestScoped")
                        || hasAnyImport(imports, "javax.enterprise", "jakarta.enterprise", "javax.inject", "jakarta.inject"),
                "cdi");
        add(signals, relativePath, "ee.platform", "EE", "Java EE / Jakarta EE", "MEDIUM",
                hasAnyImport(imports, "javax.enterprise", "jakarta.enterprise", "javax.ejb", "jakarta.ejb"), "ee");

        add(signals, relativePath, "api.jax-rs", "API", "JAX-RS", "HIGH",
                containsAny(lower, "@path", "@get", "@post", "@put", "@delete")
                        || hasAnyImport(imports, "javax.ws.rs", "jakarta.ws.rs"), "jaxrs");
        add(signals, relativePath, "api.servlet", "API", "Servlet", "HIGH",
                hasAnyImport(imports, "javax.servlet", "jakarta.servlet") || lower.contains("extends httpservlet"),
                "servlet");
        add(signals, relativePath, "api.soap", "API", "SOAP WebService", "HIGH",
                containsAny(lower, "@webservice", "javax.jws", "jakarta.jws")
                        || hasAnyImport(imports, "javax.jws", "jakarta.jws", "javax.xml.ws"), "soap");
        add(signals, relativePath, "api.openapi", "API", "OpenAPI", "MEDIUM",
                hasAnyImport(imports, "io.swagger", "org.eclipse.microprofile.openapi")
                        || containsAny(lower, "@operation", "@schema", "openapi"), "openapi");

        add(signals, relativePath, "persistence.jpa", "PERSISTENCE", "JPA Entity", "HIGH",
                annotations.contains("Entity") || hasAnyImport(imports, "javax.persistence", "jakarta.persistence"),
                "jpa");
        add(signals, relativePath, "persistence.jdbc", "PERSISTENCE", "JDBC", "MEDIUM",
                hasAnyImport(imports, "java.sql", "javax.sql") || lower.contains("datasource"), "jdbc");
        add(signals, relativePath, "persistence.liquibase", "PERSISTENCE", "Liquibase", "HIGH",
                hasAnyImport(imports, "liquibase") || lower.contains("liquibase"), "liquibase");
        add(signals, relativePath, "persistence.flyway", "PERSISTENCE", "Flyway", "HIGH",
                hasAnyImport(imports, "org.flywaydb") || lower.contains("flyway"), "flyway");

        add(signals, relativePath, "messaging.jms", "MESSAGING", "JMS", "HIGH",
                hasAnyImport(imports, "javax.jms", "jakarta.jms") || lower.contains("@jmsdestination"), "jms");
        add(signals, relativePath, "messaging.kafka", "MESSAGING", "Kafka", "HIGH",
                hasAnyImport(imports, "org.apache.kafka") || lower.contains("kafkatemplate"), "kafka");
        add(signals, relativePath, "messaging.rabbitmq", "MESSAGING", "RabbitMQ", "HIGH",
                hasAnyImport(imports, "com.rabbitmq", "org.springframework.amqp") || lower.contains("rabbittemplate"),
                "rabbit");

        add(signals, relativePath, "ui.jsf", "UI", "JSF", "MEDIUM",
                hasAnyImport(imports, "javax.faces", "jakarta.faces") || lower.contains("@managedbean"), "jsf");
        add(signals, relativePath, "ui.primefaces", "UI", "PrimeFaces", "MEDIUM",
                hasAnyImport(imports, "org.primefaces") || lower.contains("primefaces"), "primefaces");

        add(signals, relativePath, "framework.spring", "FRAMEWORK", "Spring", "HIGH",
                hasAnyImport(imports, "org.springframework"), "spring");
        add(signals, relativePath, "framework.spring-boot", "FRAMEWORK", "Spring Boot", "HIGH",
                hasAnyImport(imports, "org.springframework.boot") || annotations.contains("SpringBootApplication"),
                "spring-boot");
        add(signals, relativePath, "framework.quarkus", "FRAMEWORK", "Quarkus", "HIGH",
                hasAnyImport(imports, "io.quarkus"), "quarkus");

        add(signals, relativePath, "security.oauth2", "SECURITY", "OAuth2", "MEDIUM",
                hasAnyImport(imports, "org.springframework.security.oauth2")
                        || containsAny(lower, "oauth2", "clientregistration"), "oauth2");
        add(signals, relativePath, "security.oidc", "SECURITY", "OIDC", "MEDIUM",
                containsAny(lower, "openid", "oidc") || hasAnyImport(imports, "io.quarkus.oidc"), "oidc");
        add(signals, relativePath, "security.keycloak", "SECURITY", "Keycloak", "HIGH",
                hasAnyImport(imports, "org.keycloak") || lower.contains("keycloak"), "keycloak");

        add(signals, relativePath, "quality.health", "QUALITY", "Health checks", "MEDIUM",
                hasAnyImport(imports, "org.eclipse.microprofile.health", "org.springframework.boot.actuate.health")
                        || containsAny(lower, "@readiness", "@liveness", "/health"), "health");
        add(signals, relativePath, "quality.metrics", "QUALITY", "Metrics", "MEDIUM",
                hasAnyImport(imports, "io.micrometer", "org.eclipse.microprofile.metrics")
                        || lower.contains("meterregistry"), "metrics");
        add(signals, relativePath, "quality.tracing", "QUALITY", "Tracing", "MEDIUM",
                hasAnyImport(imports, "io.opentelemetry", "io.jaegertracing", "brave.Tracing")
                        || containsAny(lower, "tracer", "opentelemetry"), "tracing");

        return signals;
    }

    public static List<DetectedSignal> detectFromPath(String relativePath) {
        List<DetectedSignal> signals = new ArrayList<>();
        String normalized = relativePath.replace('\\', '/').toLowerCase(Locale.ROOT);
        String name = normalized.substring(normalized.lastIndexOf('/') + 1);

        if (name.equals("web.xml") || name.equals("beans.xml") || name.equals("persistence.xml")) {
            addPath(signals, relativePath, "ee.descriptor", "EE", "EE descriptor", "HIGH", "descriptor");
        }
        if (name.contains("ibm-web") || name.contains("ibm-ejb") || name.endsWith(".xmi")) {
            addPath(signals, relativePath, "ee.was-descriptor", "EE", "WebSphere descriptor", "HIGH", "was");
        }
        if (name.contains("jboss-") || name.equals("standalone.xml") || name.contains("wildfly")) {
            addPath(signals, relativePath, "ee.jboss-descriptor", "EE", "JBoss/WildFly descriptor", "HIGH", "jboss");
        }
        if (name.endsWith(".wsdl") || name.endsWith(".xsd") && normalized.contains("soap")) {
            addPath(signals, relativePath, "api.wsdl", "API", "WSDL/XSD", "HIGH", "wsdl");
        }
        if (name.contains("liquibase") || name.equals("changelog.xml") || name.equals("db.changelog-master.yaml")
                || name.equals("db.changelog-master.yml") || name.equals("db.changelog-master.xml")) {
            addPath(signals, relativePath, "persistence.liquibase", "PERSISTENCE", "Liquibase", "HIGH", "liquibase-file");
        }
        if (name.contains("flyway") || normalized.contains("db/migration/")) {
            addPath(signals, relativePath, "persistence.flyway", "PERSISTENCE", "Flyway", "HIGH", "flyway-file");
        }
        if (name.contains("openapi") || name.equals("swagger.json") || name.equals("swagger.yaml")) {
            addPath(signals, relativePath, "api.openapi", "API", "OpenAPI", "HIGH", "openapi-file");
        }
        if (name.contains("application") && (name.endsWith(".yml") || name.endsWith(".yaml") || name.endsWith(".properties"))) {
            // inspected elsewhere for content signals if needed
        }
        return signals;
    }

    public static List<DetectedSignal> detectFromConfigContent(String relativePath, String content) {
        List<DetectedSignal> signals = new ArrayList<>();
        String lower = content.toLowerCase(Locale.ROOT);
        add(signals, relativePath, "security.oauth2", "SECURITY", "OAuth2", "MEDIUM",
                lower.contains("oauth2") || lower.contains("client-id"), "oauth2-config");
        add(signals, relativePath, "security.oidc", "SECURITY", "OIDC", "MEDIUM",
                lower.contains("oidc") || lower.contains("openid-connect"), "oidc-config");
        add(signals, relativePath, "security.keycloak", "SECURITY", "Keycloak", "HIGH",
                lower.contains("keycloak"), "keycloak-config");
        add(signals, relativePath, "persistence.jdbc", "PERSISTENCE", "Datasource/JDBC", "MEDIUM",
                lower.contains("datasource") || lower.contains("jdbc:"), "jdbc-config");
        add(signals, relativePath, "quality.health", "QUALITY", "Health checks", "LOW",
                lower.contains("health"), "health-config");
        add(signals, relativePath, "quality.metrics", "QUALITY", "Metrics", "LOW",
                lower.contains("metrics") || lower.contains("micrometer"), "metrics-config");
        add(signals, relativePath, "quality.tracing", "QUALITY", "Tracing", "LOW",
                lower.contains("tracing") || lower.contains("opentelemetry") || lower.contains("jaeger"),
                "tracing-config");
        return signals;
    }

    private static void add(
            List<DetectedSignal> signals,
            String relativePath,
            String id,
            String category,
            String name,
            String confidence,
            boolean condition,
            String suffix
    ) {
        if (condition) {
            signals.add(new DetectedSignal(
                    id, category, name, confidence,
                    "src:" + relativePath + ":" + suffix,
                    "regex-heuristic"
            ));
        }
    }

    private static void addPath(
            List<DetectedSignal> signals,
            String relativePath,
            String id,
            String category,
            String name,
            String confidence,
            String suffix
    ) {
        signals.add(new DetectedSignal(
                id, category, name, confidence,
                "src:" + relativePath + ":" + suffix,
                "path-heuristic"
        ));
    }

    private static boolean containsAny(String content, String... needles) {
        for (String needle : needles) {
            if (content.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnyImport(List<String> imports, String... prefixes) {
        for (String imp : imports) {
            for (String prefix : prefixes) {
                if (imp.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Map<String, SignalRule> buildRules() {
        Map<String, SignalRule> rules = new LinkedHashMap<>();
        rules.put("spring-boot", new SignalRule("framework.spring-boot", "FRAMEWORK", "Spring Boot", "HIGH"));
        rules.put("springframework", new SignalRule("framework.spring", "FRAMEWORK", "Spring", "HIGH"));
        rules.put("quarkus", new SignalRule("framework.quarkus", "FRAMEWORK", "Quarkus", "HIGH"));
        rules.put("javax.ejb", new SignalRule("ee.ejb", "EE", "EJB", "HIGH"));
        rules.put("jakarta.ejb", new SignalRule("ee.ejb", "EE", "EJB", "HIGH"));
        rules.put("javax.persistence", new SignalRule("persistence.jpa", "PERSISTENCE", "JPA", "HIGH"));
        rules.put("jakarta.persistence", new SignalRule("persistence.jpa", "PERSISTENCE", "JPA", "HIGH"));
        rules.put("javax.ws.rs", new SignalRule("api.jax-rs", "API", "JAX-RS", "HIGH"));
        rules.put("jakarta.ws.rs", new SignalRule("api.jax-rs", "API", "JAX-RS", "HIGH"));
        rules.put("javax.servlet", new SignalRule("api.servlet", "API", "Servlet", "HIGH"));
        rules.put("jakarta.servlet", new SignalRule("api.servlet", "API", "Servlet", "HIGH"));
        rules.put("javax.faces", new SignalRule("ui.jsf", "UI", "JSF", "MEDIUM"));
        rules.put("jakarta.faces", new SignalRule("ui.jsf", "UI", "JSF", "MEDIUM"));
        rules.put("primefaces", new SignalRule("ui.primefaces", "UI", "PrimeFaces", "HIGH"));
        rules.put("javax.jms", new SignalRule("messaging.jms", "MESSAGING", "JMS", "HIGH"));
        rules.put("jakarta.jms", new SignalRule("messaging.jms", "MESSAGING", "JMS", "HIGH"));
        rules.put("kafka-clients", new SignalRule("messaging.kafka", "MESSAGING", "Kafka", "HIGH"));
        rules.put("amqp-client", new SignalRule("messaging.rabbitmq", "MESSAGING", "RabbitMQ", "HIGH"));
        rules.put("spring-rabbit", new SignalRule("messaging.rabbitmq", "MESSAGING", "RabbitMQ", "HIGH"));
        rules.put("liquibase", new SignalRule("persistence.liquibase", "PERSISTENCE", "Liquibase", "HIGH"));
        rules.put("flyway", new SignalRule("persistence.flyway", "PERSISTENCE", "Flyway", "HIGH"));
        rules.put("keycloak", new SignalRule("security.keycloak", "SECURITY", "Keycloak", "HIGH"));
        rules.put("spring-security-oauth2", new SignalRule("security.oauth2", "SECURITY", "OAuth2", "HIGH"));
        rules.put("swagger", new SignalRule("api.openapi", "API", "OpenAPI", "MEDIUM"));
        rules.put("springdoc", new SignalRule("api.openapi", "API", "OpenAPI", "HIGH"));
        rules.put("micrometer", new SignalRule("quality.metrics", "QUALITY", "Metrics", "MEDIUM"));
        rules.put("opentelemetry", new SignalRule("quality.tracing", "QUALITY", "Tracing", "MEDIUM"));
        rules.put("cxf", new SignalRule("api.soap", "API", "SOAP", "MEDIUM"));
        rules.put("jaxws", new SignalRule("api.soap", "API", "SOAP", "MEDIUM"));
        return rules;
    }

    private record SignalRule(String id, String category, String name, String confidence) {
    }

    public record DetectedSignal(
            String id,
            String category,
            String name,
            String confidence,
            String evidenceKey,
            String detector
    ) {
    }
}
