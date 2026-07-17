import { afterEach, describe, expect, it, vi } from "vitest";
import { getApiBaseUrl, parseProblemDetails } from "@/lib/api/client";

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
