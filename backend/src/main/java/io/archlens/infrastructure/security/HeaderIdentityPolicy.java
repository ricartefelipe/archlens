package io.archlens.infrastructure.security;

/**
 * Headers de identidade e de organização só são aceitos fora de produção.
 */
public final class HeaderIdentityPolicy {

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

    private static boolean isProduction(String profileLabel) {
        return profileLabel != null && "production".equalsIgnoreCase(profileLabel.trim());
    }
}
