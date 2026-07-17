package io.archlens.api.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Limite simples in-memory por IP para POST de importação.
 * Em cluster, substituir por gateway/redis (documentado no runbook).
 */
@Provider
public class ImportRateLimitFilter implements ContainerRequestFilter {

    @ConfigProperty(name = "archlens.rate-limit.import-per-minute", defaultValue = "30")
    int limitPerMinute;

    private final Map<String, Deque<Long>> windows = new ConcurrentHashMap<>();

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!"POST".equalsIgnoreCase(requestContext.getMethod())) {
            return;
        }
        String path = requestContext.getUriInfo().getPath();
        // Resource: /api/v1/assessments/{id}/import
        if (path == null || !path.contains("/assessments/") || !path.endsWith("/import")) {
            return;
        }
        int limit = limitPerMinute <= 0 ? Integer.MAX_VALUE : limitPerMinute;
        String key = clientKey(requestContext);
        long now = Instant.now().toEpochMilli();
        long cutoff = now - 60_000L;
        Deque<Long> hits = windows.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (hits) {
            while (!hits.isEmpty() && hits.peekFirst() < cutoff) {
                hits.removeFirst();
            }
            if (hits.size() >= limit) {
                requestContext.abortWith(Response.status(429)
                        .entity(Map.of(
                                "title", "Too Many Requests",
                                "status", 429,
                                "detail", "Limite de importações por minuto excedido"))
                        .type("application/problem+json")
                        .build());
                return;
            }
            hits.addLast(now);
        }
    }

    private static String clientKey(ContainerRequestContext ctx) {
        String forwarded = ctx.getHeaderString("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String user = ctx.getHeaderString("X-User");
        if (user != null && !user.isBlank()) {
            return "user:" + user;
        }
        return "anonymous";
    }
}
