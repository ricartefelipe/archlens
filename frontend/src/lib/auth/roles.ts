import type { UserRole } from "@/types";

const WRITE_ROLES: UserRole[] = ["ADMIN", "ARCHITECT"];

export function hasRole(roles: UserRole[] | undefined, role: UserRole): boolean {
  return roles?.includes(role) ?? false;
}

export function canWrite(roles: UserRole[] | undefined): boolean {
  return roles?.some((role) => WRITE_ROLES.includes(role)) ?? false;
}

export function canManageFindings(roles: UserRole[] | undefined): boolean {
  return canWrite(roles);
}

export function extractRolesFromProfile(
  profile: Record<string, unknown> | undefined,
): UserRole[] {
  if (!profile) {
    return ["VIEWER"];
  }

  const realmAccess = profile.realm_access as { roles?: string[] } | undefined;
  const rawRoles = realmAccess?.roles ?? [];
  const mapped = rawRoles
    .map((role) => role.toUpperCase())
    .filter((role): role is UserRole =>
      ["ADMIN", "ARCHITECT", "VIEWER"].includes(role),
    );

  return mapped.length > 0 ? mapped : ["VIEWER"];
}
