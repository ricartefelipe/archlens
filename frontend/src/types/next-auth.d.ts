import type { UserRole } from "@/types";
import "next-auth";
import "next-auth/jwt";

declare module "next-auth" {
  interface Session {
    accessToken?: string;
    roles: UserRole[];
  }

  interface User {
    roles?: UserRole[];
  }
}

declare module "next-auth/jwt" {
  interface JWT {
    accessToken?: string;
    roles: UserRole[];
  }
}
