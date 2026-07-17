package io.archlens.infrastructure.config;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Falha o startup quando o perfil exige hardening e a configuração crítica está ausente ou insegura.
 */
@ApplicationScoped
public class SecurityConfigValidator {

    private static final Logger LOG = Logger.getLogger(SecurityConfigValidator.class);

    private static final Set<String> FORBIDDEN_DB_PASSWORDS = Set.of(
            "archlens", "password", "postgres", "admin", "sa", "changeme", "");

    private static final Set<String> FORBIDDEN_OIDC_SECRETS = Set.of(
            "archlens-api-secret", "changeme", "secret", "");

    @ConfigProperty(name = "archlens.security.require-hardened", defaultValue = "false")
    boolean requireHardened;

    @ConfigProperty(name = "archlens.local-header-auth.enabled", defaultValue = "false")
    boolean localHeaderAuth;

    @ConfigProperty(name = "quarkus.oidc.enabled", defaultValue = "false")
    boolean oidcEnabled;

    @ConfigProperty(name = "quarkus.tls.oidc.trust-all", defaultValue = "false")
    boolean oidcTlsTrustAll;

    @ConfigProperty(name = "quarkus.datasource.password")
    String dbPassword;

    @ConfigProperty(name = "quarkus.oidc.credentials.secret")
    String oidcClientSecret;

    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String oidcAuthServerUrl;

    @ConfigProperty(name = "quarkus.oidc.token.issuer")
    String oidcIssuer;

    @ConfigProperty(name = "quarkus.http.cors.origins")
    String corsOrigins;

    @ConfigProperty(name = "archlens.profile.label", defaultValue = "unspecified")
    String profileLabel;

    void onStart(@Observes StartupEvent event) {
        LOG.infof("ArchLens profile label=%s hardened=%s oidc=%s headerAuth=%s",
                profileLabel, requireHardened, oidcEnabled, localHeaderAuth);

        if (!requireHardened) {
            if (localHeaderAuth) {
                LOG.warn("Auth por headers locais ATIVA — adequado só para desenvolvimento/demo/teste");
            }
            return;
        }

        List<String> errors = new ArrayList<>();
        if (localHeaderAuth) {
            errors.add("archlens.local-header-auth.enabled deve ser false em produção");
        }
        if (!oidcEnabled) {
            errors.add("OIDC deve estar habilitado em produção (ARCHLENS_OIDC_ENABLED=true)");
        }
        if (oidcTlsTrustAll) {
            errors.add("quarkus.tls.oidc.trust-all não pode ser true em produção");
        }
        if (isBlank(dbPassword) || FORBIDDEN_DB_PASSWORDS.contains(dbPassword.toLowerCase(Locale.ROOT))) {
            errors.add("ARCHLENS_DB_PASSWORD ausente ou usa valor padrão inseguro");
        }
        if (isBlank(oidcClientSecret)
                || FORBIDDEN_OIDC_SECRETS.contains(oidcClientSecret.toLowerCase(Locale.ROOT))) {
            errors.add("ARCHLENS_OIDC_CLIENT_SECRET ausente ou usa valor padrão de demo");
        }
        if (isBlank(oidcAuthServerUrl) || oidcAuthServerUrl.contains("localhost")) {
            errors.add("ARCHLENS_OIDC_URL deve apontar para o IdP real (não localhost)");
        }
        if (isBlank(oidcIssuer)) {
            errors.add("ARCHLENS_OIDC_ISSUER é obrigatório em produção");
        }
        if (isBlank(corsOrigins) || corsOrigins.contains("*")) {
            errors.add("ARCHLENS_CORS_ORIGINS deve ser restritivo (sem *)");
        }

        if (!errors.isEmpty()) {
            String message = "Configuração de produção inválida: " + String.join("; ", errors);
            LOG.error(message);
            throw new IllegalStateException(message);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
