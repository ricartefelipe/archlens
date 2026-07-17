package io.archlens.infrastructure.health;

import io.archlens.application.service.RuleLoader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class RulesHealthCheck implements HealthCheck {

    @Inject
    RuleLoader ruleLoader;

    @Override
    public HealthCheckResponse call() {
        int count = ruleLoader.count();
        if (count <= 0) {
            return HealthCheckResponse.named("archlens-rules")
                    .down()
                    .withData("rulesLoaded", count)
                    .build();
        }
        return HealthCheckResponse.named("archlens-rules")
                .up()
                .withData("rulesLoaded", count)
                .build();
    }
}
