/**
 * Renovação server-side de access token Keycloak (Authorization Code Flow).
 * O refresh token nunca é devolvido na sessão pública do NextAuth.
 */
export interface KeycloakRefreshResult {
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresAt: number;
}

export async function refreshKeycloakAccessToken(
  refreshToken: string,
  issuer: string,
  clientId: string,
  clientSecret: string,
  request: typeof fetch = fetch,
): Promise<KeycloakRefreshResult> {
  if (!refreshToken || !issuer || !clientId || !clientSecret) {
    throw new Error("Configuração de renovação OIDC incompleta");
  }

  const endpoint = new URL(issuer.replace(/\/$/, "") + "/protocol/openid-connect/token");
  if (endpoint.protocol !== "https:" && !["localhost", "127.0.0.1"].includes(endpoint.hostname)) {
    throw new Error("Endpoint de renovação OIDC deve utilizar HTTPS");
  }

  const response = await request(endpoint.toString(), {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "refresh_token",
      client_id: clientId,
      client_secret: clientSecret,
      refresh_token: refreshToken,
    }),
    cache: "no-store",
    signal: AbortSignal.timeout(10_000),
  });

  if (!response.ok) {
    throw new Error("Keycloak recusou a renovação do access token");
  }

  const payload: unknown = await response.json();
  if (!payload || typeof payload !== "object") {
    throw new Error("Resposta OIDC inválida");
  }
  const data = payload as Record<string, unknown>;
  if (typeof data.access_token !== "string" || !data.access_token ||
      typeof data.expires_in !== "number" || !Number.isFinite(data.expires_in) ||
      data.expires_in <= 0) {
    throw new Error("Resposta OIDC sem access token ou expiração válidos");
  }

  return {
    accessToken: data.access_token,
    refreshToken: typeof data.refresh_token === "string" && data.refresh_token
      ? data.refresh_token : refreshToken,
    accessTokenExpiresAt: Date.now() + data.expires_in * 1000,
  };
}
