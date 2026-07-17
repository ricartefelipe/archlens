import { describe, expect, it } from "vitest";
import {
  canManageFindings,
  canWrite,
  extractRolesFromProfile,
  hasRole,
} from "@/lib/auth/roles";

describe("roles", () => {
  it("VIEWER não escreve", () => {
    expect(canWrite(["VIEWER"])).toBe(false);
    expect(canManageFindings(["VIEWER"])).toBe(false);
  });

  it("ARCHITECT e ADMIN escrevem", () => {
    expect(canWrite(["ARCHITECT"])).toBe(true);
    expect(canWrite(["ADMIN"])).toBe(true);
    expect(hasRole(["ADMIN"], "ADMIN")).toBe(true);
  });

  it("extrai papéis do realm_access", () => {
    const roles = extractRolesFromProfile({
      realm_access: { roles: ["architect", "viewer"] },
    });
    expect(roles).toEqual(["ARCHITECT", "VIEWER"]);
  });

  it("fallback VIEWER sem papéis", () => {
    expect(extractRolesFromProfile({})).toEqual(["VIEWER"]);
  });
});
