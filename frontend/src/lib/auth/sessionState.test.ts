import { describe, expect, it } from "vitest";
import { isSessionUsable } from "./sessionState";

describe("isSessionUsable", () => {
  it("aceita sessão sem erro", () => {
    expect(isSessionUsable({})).toBe(true);
  });

  it("rejeita ausência de sessão", () => {
    expect(isSessionUsable(null)).toBe(false);
    expect(isSessionUsable(undefined)).toBe(false);
  });

  it("rejeita sessão cuja renovação do token falhou", () => {
    expect(isSessionUsable({ error: "RefreshAccessTokenError" })).toBe(false);
  });
});
