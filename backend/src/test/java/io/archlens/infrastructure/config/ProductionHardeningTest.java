package io.archlens.infrastructure.config;

import io.archlens.api.filter.ImportRateLimitFilter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionHardeningTest {

    private static final SecurityConfigValidator.HardeningInput SAFE = new SecurityConfigValidator.HardeningInput(
            false,
            true,
            true,
            false,
            "cliente-db-9f3a",
            "cliente-oidc-7c21",
            "https://idp.cliente.example/realms/archlens",
            "https://idp.cliente.example/realms/archlens",
            "https://app.cliente.example");

    @Test
    void forgedForwardedHeaderDoesNotSplitTheRateLimitBucket() {
        String withoutTrust = ImportRateLimitFilter.clientKey(false, "203.0.113.9", false, "intruso", "10.0.0.8");
        String otherForwarded = ImportRateLimitFilter.clientKey(false, "198.51.100.4", false, "outro", "10.0.0.8");
        assertTrue(withoutTrust.equals(otherForwarded));
        assertTrue(withoutTrust.equals("ip:10.0.0.8"));

        String trusted = ImportRateLimitFilter.clientKey(true, "203.0.113.9, 10.0.0.1", false, "intruso", "10.0.0.8");
        assertTrue(trusted.equals("fwd:203.0.113.9"));
    }

    @Test
    void safeProductionSettingsHaveNoErrors() {
        assertTrue(SecurityConfigValidator.productionErrors(SAFE).isEmpty());
    }

    @Test
    void eachUnsafeProductionSettingIsRejected() {
        assertRejected(new SecurityConfigValidator.HardeningInput(
                true, SAFE.oidcEnabled(), SAFE.oidcTenantEnabled(), SAFE.oidcTlsTrustAll(), SAFE.dbPassword(),
                SAFE.oidcClientSecret(), SAFE.oidcAuthServerUrl(), SAFE.oidcIssuer(), SAFE.corsOrigins()),
                "local-header-auth");
        assertRejected(new SecurityConfigValidator.HardeningInput(
                SAFE.localHeaderAuth(), false, SAFE.oidcTenantEnabled(), SAFE.oidcTlsTrustAll(), SAFE.dbPassword(),
                SAFE.oidcClientSecret(), SAFE.oidcAuthServerUrl(), SAFE.oidcIssuer(), SAFE.corsOrigins()),
                "OIDC");
        assertRejected(new SecurityConfigValidator.HardeningInput(
                SAFE.localHeaderAuth(), SAFE.oidcEnabled(), false, SAFE.oidcTlsTrustAll(), SAFE.dbPassword(),
                SAFE.oidcClientSecret(), SAFE.oidcAuthServerUrl(), SAFE.oidcIssuer(), SAFE.corsOrigins()),
                "Tenant OIDC");
        assertRejected(new SecurityConfigValidator.HardeningInput(
                SAFE.localHeaderAuth(), SAFE.oidcEnabled(), SAFE.oidcTenantEnabled(), true, SAFE.dbPassword(),
                SAFE.oidcClientSecret(), SAFE.oidcAuthServerUrl(), SAFE.oidcIssuer(), SAFE.corsOrigins()),
                "trust-all");
        assertRejected(new SecurityConfigValidator.HardeningInput(
                SAFE.localHeaderAuth(), SAFE.oidcEnabled(), SAFE.oidcTenantEnabled(), SAFE.oidcTlsTrustAll(), "archlens",
                SAFE.oidcClientSecret(), SAFE.oidcAuthServerUrl(), SAFE.oidcIssuer(), SAFE.corsOrigins()),
                "ARCHLENS_DB_PASSWORD");
        assertRejected(new SecurityConfigValidator.HardeningInput(
                SAFE.localHeaderAuth(), SAFE.oidcEnabled(), SAFE.oidcTenantEnabled(), SAFE.oidcTlsTrustAll(), "",
                SAFE.oidcClientSecret(), SAFE.oidcAuthServerUrl(), SAFE.oidcIssuer(), SAFE.corsOrigins()),
                "ARCHLENS_DB_PASSWORD");
        assertRejected(new SecurityConfigValidator.HardeningInput(
                SAFE.localHeaderAuth(), SAFE.oidcEnabled(), SAFE.oidcTenantEnabled(), SAFE.oidcTlsTrustAll(), SAFE.dbPassword(),
                "archlens-api-secret", SAFE.oidcAuthServerUrl(), SAFE.oidcIssuer(), SAFE.corsOrigins()),
                "ARCHLENS_OIDC_CLIENT_SECRET");
        assertRejected(new SecurityConfigValidator.HardeningInput(
                SAFE.localHeaderAuth(), SAFE.oidcEnabled(), SAFE.oidcTenantEnabled(), SAFE.oidcTlsTrustAll(), SAFE.dbPassword(),
                SAFE.oidcClientSecret(), "http://localhost:8081/realms/archlens", SAFE.oidcIssuer(), SAFE.corsOrigins()),
                "localhost");
        assertRejected(new SecurityConfigValidator.HardeningInput(
                SAFE.localHeaderAuth(), SAFE.oidcEnabled(), SAFE.oidcTenantEnabled(), SAFE.oidcTlsTrustAll(), SAFE.dbPassword(),
                SAFE.oidcClientSecret(), SAFE.oidcAuthServerUrl(), "  ", SAFE.corsOrigins()),
                "ARCHLENS_OIDC_ISSUER");
        assertRejected(new SecurityConfigValidator.HardeningInput(
                SAFE.localHeaderAuth(), SAFE.oidcEnabled(), SAFE.oidcTenantEnabled(), SAFE.oidcTlsTrustAll(), SAFE.dbPassword(),
                SAFE.oidcClientSecret(), SAFE.oidcAuthServerUrl(), SAFE.oidcIssuer(), "*"),
                "CORS");
    }

    private static void assertRejected(SecurityConfigValidator.HardeningInput input, String fragment) {
        List<String> errors = SecurityConfigValidator.productionErrors(input);
        assertTrue(errors.stream().anyMatch(error -> error.contains(fragment)), errors.toString());
    }
}
