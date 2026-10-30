import { describe, expect, it, vi } from "vitest";
import { refreshKeycloakAccessToken } from "./refreshKeycloakToken";

describe("refreshKeycloakAccessToken", () => {
  const endpoint = "https://idp.example.com/realms/archlens";
  const secret = "test-client-secret";

  it("renova no endpoint do realm, mantendo refresh token quando não há rotação", async () => {
    const request = vi.fn(async () => ({
      ok: true,
      json: async () => ({ access_token: "signed-new-token", expires_in: 300 }),
    })) as unknown as typeof fetch;
    const before = Date.now();

    const result = await refreshKeycloakAccessToken("old-refresh", endpoint, "archlens-frontend", secret, request);

    expect(result.accessToken).toBe("signed-new-token");
    expect(result.refreshToken).toBe("old-refresh");
    expect(result.accessTokenExpiresAt).toBeGreaterThanOrEqual(before + 300_000);
    const [url, init] = vi.mocked(request).mock.calls[0];
    expect(url).toBe(endpoint + "/protocol/openid-connect/token");
    expect((init?.body as URLSearchParams).get("grant_type")).toBe("refresh_token");
    expect((init?.body as URLSearchParams).get("refresh_token")).toBe("old-refresh");
  });

  it("aceita rotação do refresh token", async () => {
    const request = vi.fn(async () => ({
      ok: true,
      json: async () => ({ access_token: "new-token", refresh_token: "rotated-refresh", expires_in: 60 }),
    })) as unknown as typeof fetch;
    const result = await refreshKeycloakAccessToken("old-refresh", endpoint, "frontend", secret, request);
    expect(result.refreshToken).toBe("rotated-refresh");
  });

  it("falha de forma fechada se Keycloak rejeitar ou retornar um token inválido", async () => {
    const rejected = vi.fn(async () => ({ ok: false })) as unknown as typeof fetch;
    await expect(refreshKeycloakAccessToken("r", endpoint, "front", secret, rejected)).rejects.toThrow("recusou");
    const malformed = vi.fn(async () => ({ ok: true, json: async () => ({ access_token: "x", expires_in: 0 }) })) as unknown as typeof fetch;
    await expect(refreshKeycloakAccessToken("r", endpoint, "front", secret, malformed)).rejects.toThrow("expiração válidos");
  });

  it("recusa transporte HTTP fora do ambiente local", async () => {
    const request = vi.fn() as unknown as typeof fetch;
    await expect(refreshKeycloakAccessToken("r", "http://idp.example.com/realms/archlens", "front", secret, request)).rejects.toThrow("HTTPS");
    expect(request).not.toHaveBeenCalled();
  });
});
