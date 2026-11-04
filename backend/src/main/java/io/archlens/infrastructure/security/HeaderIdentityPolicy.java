package io.archlens.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Headers de identidade e de organização só são aceitos fora de produção e, mesmo assim, só quando a
 * requisição traz o segredo compartilhado com o servidor do frontend ({@value #PROXY_SECRET_HEADER}).
 * <p>
 * O navegador nunca conhece esse segredo: ele fala com o frontend, que confere a sessão e repassa a
 * identidade ao backend. Sem o segredo, {@code X-User}, {@code X-Roles} e {@code X-Organization-Id}
 * são ignorados, e a API responde 401 como em produção.
 */
public final class HeaderIdentityPolicy {

    public static final String PROXY_SECRET_HEADER = "X-ArchLens-Proxy-Secret";

    /** Tamanho mínimo do segredo compartilhado entre frontend e backend. */
    public static final int MIN_SECRET_LENGTH = 32;

    private HeaderIdentityPolicy() {
    }

    public static boolean headerIdentityAllowed(boolean localHeaderAuthEnabled, String profileLabel) {
        if (!localHeaderAuthEnabled) {
            return false;
        }
        return !isProduction(profileLabel);
    }

    public static boolean organizationHeaderTrusted(String profileLabel) {
        return !isProduction(profileLabel);
    }

    /**
     * Compara o segredo recebido com o configurado em tempo constante. Segredo ausente, vazio ou
     * curto demais nunca autoriza.
     */
    public static boolean proxySecretMatches(String expected, String provided) {
        if (expected == null || expected.length() < MIN_SECRET_LENGTH || provided == null || provided.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isProduction(String profileLabel) {
        return profileLabel != null && "production".equalsIgnoreCase(profileLabel.trim());
    }
}
