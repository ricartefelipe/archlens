const DEMO_PASSWORDS = new Set([
  "admin123",
  "architect123",
  "viewer123",
  "admin",
  "changeme",
  "password",
]);

export function readEnv(name: string): string | undefined {
  const value = process.env[name];
  if (value == null) {
    return undefined;
  }
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : undefined;
}

export function readEnvFlag(name: string): boolean {
  return readEnv(name) === "true";
}

/** Login local só existe fora do perfil de produção e com senha vinda do ambiente. */
export function isLocalAuthAllowed(): boolean {
  return readEnvFlag("AUTH_LOCAL_ENABLED") && readEnv("ARCHLENS_PROFILE_LABEL") !== "production";
}

/**
 * Senha de login local. Valores de demonstração só valem com ARCHLENS_ALLOW_DEMO_CREDENTIALS=true.
 * Sem variável, o usuário local não autentica.
 */
export function resolveLocalPassword(configured: string | undefined): string | null {
  const value = configured?.trim() ?? "";
  if (!value) {
    return null;
  }
  if (DEMO_PASSWORDS.has(value) && !readEnvFlag("ARCHLENS_ALLOW_DEMO_CREDENTIALS")) {
    return null;
  }
  return value;
}
