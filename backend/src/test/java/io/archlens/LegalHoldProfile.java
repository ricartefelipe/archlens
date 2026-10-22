package io.archlens;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class LegalHoldProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("archlens.retention.legal-hold", "true");
    }
}
