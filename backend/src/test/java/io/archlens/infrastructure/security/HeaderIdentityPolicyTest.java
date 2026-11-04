package io.archlens.infrastructure.security;

import io.archlens.infrastructure.config.SecurityConfigValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeaderIdentityPolicyTest {

    private static final String SECRET = "segredo-compartilhado-com-o-frontend-01";

    @Test
    void onlyTheExactSecretAuthorizes() {
        assertTrue(HeaderIdentityPolicy.proxySecretMatches(SECRET, SECRET));
        assertFalse(HeaderIdentityPolicy.proxySecretMatches(SECRET, SECRET + "x"));
        assertFalse(HeaderIdentityPolicy.proxySecretMatches(SECRET, SECRET.substring(1)));
        assertFalse(HeaderIdentityPolicy.proxySecretMatches(SECRET, ""));
        assertFalse(HeaderIdentityPolicy.proxySecretMatches(SECRET, null));
    }

    @Test
    void missingOrShortConfiguredSecretNeverAuthorizes() {
        assertFalse(HeaderIdentityPolicy.proxySecretMatches(null, "qualquer"));
        assertFalse(HeaderIdentityPolicy.proxySecretMatches("", ""));
        assertFalse(HeaderIdentityPolicy.proxySecretMatches("curto", "curto"));
    }

    @Test
    void productionNeverAllowsHeaderIdentity() {
        assertFalse(HeaderIdentityPolicy.headerIdentityAllowed(true, "production"));
        assertTrue(HeaderIdentityPolicy.headerIdentityAllowed(true, "demo"));
        assertFalse(HeaderIdentityPolicy.headerIdentityAllowed(false, "demo"));
    }

    @Test
    void headerAuthWithoutStrongSecretFailsStartup() {
        assertFalse(SecurityConfigValidator.headerAuthErrors(true, null).isEmpty());
        assertFalse(SecurityConfigValidator.headerAuthErrors(true, "   ").isEmpty());
        assertFalse(SecurityConfigValidator.headerAuthErrors(true, "curto-demais").isEmpty());
        assertTrue(SecurityConfigValidator.headerAuthErrors(true, SECRET).isEmpty());
        assertTrue(SecurityConfigValidator.headerAuthErrors(false, null).isEmpty());
    }
}
