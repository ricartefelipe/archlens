package io.archlens;

import io.archlens.infrastructure.security.HeaderIdentityPolicy;

/** Identidade local dos testes: o mesmo segredo de {@code %test.archlens.local-header-auth.secret}. */
final class TestIdentity {

    static final String SECRET_HEADER = HeaderIdentityPolicy.PROXY_SECRET_HEADER;
    static final String SECRET = "segredo-dos-testes-de-integracao-0001";

    private TestIdentity() {
    }
}
