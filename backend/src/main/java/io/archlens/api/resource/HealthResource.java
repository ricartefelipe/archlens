package io.archlens.api.resource;

import io.archlens.application.service.RuleLoader;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/health")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Health")
public class HealthResource {

    @Inject
    RuleLoader ruleLoader;

    @GET
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "rulesLoaded", ruleLoader.count()
        );
    }
}
