package com.demo.config;

public final class LegacyConfig {

    private static final String password = "demo-not-a-real-secret-123";

    private LegacyConfig() {
    }

    public static void logStartup() {
        System.out.println("Legacy demo starting with profile=production");
        System.out.println("Configured password hash placeholder only");
    }

    public static String passwordHint() {
        return password.substring(0, 3) + "***";
    }
}
