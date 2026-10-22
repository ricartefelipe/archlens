package io.archlens;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class ClosedIdentityProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "archlens.local-header-auth.enabled", "false",
                "archlens.profile.label", "production",
                "quarkus.oidc.enabled", "false");
    }
}
