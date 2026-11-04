import { describe, expect, it } from "vitest";
import {
  crossSiteWrite,
  forwardedRequestHeaders,
  identityFromSession,
  PROXY_SECRET_HEADER,
  upstreamPath,
} from "@/lib/api/bff";

const SECRET = "segredo-compartilhado-com-o-backend-01";

describe("upstreamPath", () => {
  it("monta o caminho da API a partir dos segmentos", () => {
    expect(upstreamPath(["assessments", "abc", "findings"])).toBe("/assessments/abc/findings");
    expect(upstreamPath(["assessments", "abc", "findings", "export.csv"])).toBe(
      "/assessments/abc/findings/export.csv",
    );
  });

  it("recusa caminho vazio, . e .. e barras embutidas", () => {
    expect(upstreamPath([])).toBeNull();
    expect(upstreamPath(["assessments", ".."])).toBeNull();
    expect(upstreamPath([".", "audit"])).toBeNull();
    expect(upstreamPath(["assessments", ""])).toBeNull();
    expect(upstreamPath(["..%2f", "x"])).not.toBeNull();
    expect(upstreamPath(["a/b"])).toBeNull();
    expect(upstreamPath(["a\\b"])).toBeNull();
  });
});

describe("identityFromSession", () => {
  it("sem sessão ou com sessão inválida responde 401", () => {
    expect(identityFromSession(null, SECRET)).toMatchObject({ ok: false, status: 401 });
    expect(
      identityFromSession({ authProvider: "local", error: "RefreshAccessTokenError" }, SECRET),
    ).toMatchObject({ ok: false, status: 401 });
  });

  it("login local: identidade da sessão com o segredo do servidor", () => {
    const result = identityFromSession(
      { authProvider: "local", roles: ["ARCHITECT", "VIEWER"], user: { email: "architect@archlens.local" } },
      SECRET,
    );
    expect(result).toEqual({
      ok: true,
      headers: { "X-User": "architect", "X-Roles": "ARCHITECT,VIEWER", [PROXY_SECRET_HEADER]: SECRET },
    });
  });

  it("login local sem papéis vira VIEWER, nunca ADMIN", () => {
    const result = identityFromSession({ authProvider: "local", user: { email: "x@archlens.local" } }, SECRET);
    expect(result.ok && result.headers["X-Roles"]).toBe("VIEWER");
  });

  it("login local sem segredo configurado falha fechado", () => {
    expect(
      identityFromSession({ authProvider: "local", user: { email: "x@archlens.local" } }, undefined),
    ).toMatchObject({ ok: false, status: 500 });
  });

  it("OIDC usa o token da sessão e não manda identidade local", () => {
    const result = identityFromSession({ authProvider: "oidc", accessToken: "tok", roles: ["ADMIN"] }, SECRET);
    expect(result).toEqual({ ok: true, headers: { Authorization: "Bearer tok" } });
  });
});

describe("forwardedRequestHeaders", () => {
  it("descarta identidade, autorização e segredo enviados pelo navegador", () => {
    const incoming = new Headers({
      "X-User": "intruso",
      "X-Roles": "ADMIN",
      "X-Organization-Id": "11111111-1111-4111-8111-111111111111",
      Authorization: "Bearer forjado",
      [PROXY_SECRET_HEADER]: "chute",
      "Content-Type": "application/json",
      "Idempotency-Key": "k1",
      "X-Correlation-Id": "c1",
      Cookie: "authjs.session-token=abc",
    });
    expect(forwardedRequestHeaders(incoming)).toEqual({
      "content-type": "application/json",
      "idempotency-key": "k1",
      "x-correlation-id": "c1",
    });
  });
});

describe("crossSiteWrite", () => {
  it("bloqueia escrita vinda de outro site e libera leitura e mesma origem", () => {
    expect(crossSiteWrite("POST", "cross-site")).toBe(true);
    expect(crossSiteWrite("DELETE", "cross-site")).toBe(true);
    expect(crossSiteWrite("GET", "cross-site")).toBe(false);
    expect(crossSiteWrite("POST", "same-origin")).toBe(false);
    expect(crossSiteWrite("POST", null)).toBe(false);
  });
});
