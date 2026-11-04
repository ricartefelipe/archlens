import { afterEach, describe, expect, it, vi } from "vitest";
import { applyIdentityPolicy, getApiBaseUrl, parseProblemDetails } from "@/lib/api/client";

describe("getApiBaseUrl", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it("no servidor usa ARCHLENS_API_INTERNAL_URL quando definida", () => {
    vi.stubEnv("ARCHLENS_API_INTERNAL_URL", "http://backend:8080/api/v1");
    vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://localhost:8080/api/v1");
    expect(getApiBaseUrl()).toBe("http://backend:8080/api/v1");
  });

  it("cai na URL pública quando não há URL interna", () => {
    vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://localhost:8080/api/v1");
    expect(getApiBaseUrl()).toBe("http://localhost:8080/api/v1");
  });

  it("no navegador usa a ponte do próprio frontend, nunca a API direta", () => {
    vi.stubGlobal("window", {});
    vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://localhost:8080/api/v1");
    try {
      expect(getApiBaseUrl()).toBe("/bff/v1");
    } finally {
      vi.unstubAllGlobals();
    }
  });
});

describe("applyIdentityPolicy", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it("no navegador remove identidade e token", () => {
    expect(
      applyIdentityPolicy(
        {
          Accept: "application/json",
          "X-User": "admin",
          "X-Roles": "ADMIN",
          "X-Organization-Id": "o1",
          Authorization: "Bearer t",
          "X-Correlation-Id": "c1",
        },
        true,
      ),
    ).toEqual({ Accept: "application/json", "X-Correlation-Id": "c1" });
  });

  it("no servidor, identidade local segue com o segredo compartilhado", () => {
    vi.stubEnv("ARCHLENS_LOCAL_HEADER_AUTH_SECRET", "segredo-compartilhado-com-o-backend-01");
    expect(applyIdentityPolicy({ "X-User": "architect", "X-Roles": "ARCHITECT" }, false)).toEqual({
      "X-User": "architect",
      "X-Roles": "ARCHITECT",
      "X-ArchLens-Proxy-Secret": "segredo-compartilhado-com-o-backend-01",
    });
  });

  it("no servidor, chamada OIDC não recebe o segredo", () => {
    vi.stubEnv("ARCHLENS_LOCAL_HEADER_AUTH_SECRET", "segredo-compartilhado-com-o-backend-01");
    expect(applyIdentityPolicy({ Authorization: "Bearer t" }, false)).toEqual({ Authorization: "Bearer t" });
  });
});

describe("parseProblemDetails", () => {
  it("interpreta application/problem+json", () => {
    const error = parseProblemDetails(403, "Forbidden", {
      type: "about:blank",
      title: "Acesso negado",
      status: 403,
      detail: "Permissão insuficiente",
      correlationId: "corr-1",
    });

    expect(error.status).toBe(403);
    expect(error.message).toBe("Permissão insuficiente");
    expect(error.title).toBe("Acesso negado");
    expect(error.correlationId).toBe("corr-1");
  });

  it("não inventa dados quando o corpo está vazio", () => {
    const error = parseProblemDetails(500, "Internal Server Error", null);
    expect(error.status).toBe(500);
    expect(error.message).toBe("Internal Server Error");
    expect(error.correlationId).toBeUndefined();
  });

  it("mapeia 401 para mensagem de reautenticação", () => {
    const error = parseProblemDetails(401, "Unauthorized", {
      title: "Não autenticado",
      status: 401,
    });
    expect(error.message).toMatch(/login|autentic/i);
  });
});
