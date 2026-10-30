package io.archlens.infrastructure.adapter;

import io.archlens.application.port.NarrativeGenerator;
import io.quarkus.arc.lookup.LookupIfProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Narrativa opcional. Desligada por padrão. Quando ligada, envia só contagens agregadas,
 * com timeout e teto de tokens, e volta ao texto determinístico se a chamada falhar.
 */
@ApplicationScoped
@LookupIfProperty(name = "archlens.llm.enabled", stringValue = "true")
public class OpenAiCompatibleNarrativeGenerator implements NarrativeGenerator {

    private static final Logger LOG = Logger.getLogger(OpenAiCompatibleNarrativeGenerator.class);

    @ConfigProperty(name = "archlens.llm.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "archlens.llm.base-url")
    java.util.Optional<String> baseUrl;

    @ConfigProperty(name = "archlens.llm.api-key")
    java.util.Optional<String> apiKey;

    @ConfigProperty(name = "archlens.llm.model", defaultValue = "gpt-4o-mini")
    String model;

    @ConfigProperty(name = "archlens.llm.timeout-ms", defaultValue = "4000")
    long timeoutMs;

    @ConfigProperty(name = "archlens.llm.max-output-tokens", defaultValue = "180")
    int maxOutputTokens;

    @Inject
    TemplateNarrativeGenerator fallback;

    @Override
    public String generateExecutiveSummary(UUID assessmentId, Map<String, Object> context) {
        String deterministic = fallback.generateExecutiveSummary(assessmentId, context);
        if (!enabled || baseUrl.isEmpty() || baseUrl.get().isBlank() || apiKey.isEmpty() || apiKey.get().isBlank()) {
            return deterministic;
        }
        String prompt = MinimizedNarrativePrompt.build(
                findingCount(deterministic),
                dimension(deterministic),
                score(deterministic)
        );
        try {
            String narrative = requestNarrative(prompt);
            if (narrative == null || narrative.isBlank()) {
                return deterministic;
            }
            return narrative.trim() + " Narrativa opcional de provedor externo; as tabelas determinísticas do relatório prevalecem.";
        } catch (Exception ex) {
            LOG.warnf("Narrativa externa indisponível; usando texto determinístico (%s)", ex.getClass().getSimpleName());
            return deterministic;
        }
    }

    private String requestNarrative(String prompt) throws Exception {
        String endpoint = baseUrl.get().replaceAll("/$", "") + "/chat/completions";
        String body = "{\"model\":" + json(model)
                + ",\"max_tokens\":" + Math.max(1, maxOutputTokens)
                + ",\"messages\":[{\"role\":\"user\",\"content\":" + json(prompt) + "}]}";
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofMillis(Math.max(500, timeoutMs)))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey.get())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(500, timeoutMs)))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            LOG.warnf("Provedor de narrativa respondeu status=%d", response.statusCode());
            return null;
        }
        return extractContent(response.body());
    }

    public static String extractContent(String body) {
        if (body == null) {
            return null;
        }
        String marker = "\"content\":";
        int index = body.indexOf(marker);
        if (index < 0) {
            return null;
        }
        int start = body.indexOf('"', index + marker.length());
        if (start < 0) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        boolean escaped = false;
        for (int i = start + 1; i < body.length(); i++) {
            char current = body.charAt(i);
            if (escaped) {
                out.append(current);
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"') {
                break;
            }
            out.append(current);
        }
        return out.toString();
    }

    private static String json(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static int findingCount(String text) {
        Matcher matcher = Pattern.compile("identificou (\\d+) achados").matcher(text == null ? "" : text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private static String dimension(String text) {
        Matcher matcher = Pattern.compile("atenção é ([A-Za-z0-9_]+) \\(score ").matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group(1) : "N/A";
    }

    private static String score(String text) {
        Matcher matcher = Pattern.compile("\\(score ([^)]+)\\)").matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group(1) : "0";
    }
}
