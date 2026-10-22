import { afterEach, describe, expect, it } from "vitest";
import {
  isLocalAuthAllowed,
  resolveLocalPassword,
} from "@/lib/auth/localCredentials";

describe("resolveLocalPassword", () => {
  afterEach(() => {
    delete process.env.ARCHLENS_ALLOW_DEMO_CREDENTIALS;
    delete process.env.AUTH_LOCAL_ENABLED;
    delete process.env.ARCHLENS_PROFILE_LABEL;
  });

  it("recusa senha ausente", () => {
    expect(resolveLocalPassword(undefined)).toBeNull();
    expect(resolveLocalPassword("  ")).toBeNull();
  });

  it("recusa senha de demonstração sem opt-in explícito", () => {
    expect(resolveLocalPassword("admin123")).toBeNull();
    expect(resolveLocalPassword("architect123")).toBeNull();
  });

  it("aceita senha de demonstração só com a flag", () => {
    process.env.ARCHLENS_ALLOW_DEMO_CREDENTIALS = "true";
    expect(resolveLocalPassword("viewer123")).toBe("viewer123");
  });

  it("aceita senha própria sem a flag de demo", () => {
    expect(resolveLocalPassword("senha-do-cliente-que-nao-e-demo")).toBe(
      "senha-do-cliente-que-nao-e-demo",
    );
  });
});

describe("isLocalAuthAllowed", () => {
  afterEach(() => {
    delete process.env.AUTH_LOCAL_ENABLED;
    delete process.env.ARCHLENS_PROFILE_LABEL;
  });

  it("fica desligado em perfil production mesmo com a flag", () => {
    process.env.AUTH_LOCAL_ENABLED = "true";
    process.env.ARCHLENS_PROFILE_LABEL = "production";
    expect(isLocalAuthAllowed()).toBe(false);
  });

  it("liga apenas quando explicitamente habilitado fora de production", () => {
    process.env.AUTH_LOCAL_ENABLED = "true";
    process.env.ARCHLENS_PROFILE_LABEL = "demo";
    expect(isLocalAuthAllowed()).toBe(true);
  });
});
