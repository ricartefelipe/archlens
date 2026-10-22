import type { UserRole } from "@/types";
import "next-auth";
import "next-auth/jwt";

declare module "next-auth" {
  interface Session {
    accessToken?: string;
    error?: "RefreshAccessTokenError";
    authProvider?: "local" | "oidc";
    roles: UserRole[];
  }

  interface User {
    roles?: UserRole[];
  }
}

declare module "next-auth/jwt" {
  interface JWT {
    accessToken?: string;
    refreshToken?: string;
    accessTokenExpiresAt?: number;
    authError?: "RefreshAccessTokenError";
    authProvider?: "local" | "oidc";
    roles: UserRole[];
  }
}
