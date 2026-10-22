import NextAuth from "next-auth";
import Credentials from "next-auth/providers/credentials";
import Keycloak from "next-auth/providers/keycloak";
import { extractRolesFromProfile } from "@/lib/auth/roles";
import {
  isLocalAuthAllowed,
  readEnv,
  resolveLocalPassword,
} from "@/lib/auth/localCredentials";
import type { UserRole } from "@/types";

function keycloakConfiguredNow(): boolean {
  return Boolean(readEnv("KEYCLOAK_CLIENT_ID") && readEnv("KEYCLOAK_ISSUER"));
}

function loadLocalUsers(): Record<
  string,
  { password: string; roles: UserRole[]; name: string }
> {
  if (!isLocalAuthAllowed()) {
    return {};
  }
  const entries: Array<[string, UserRole[], string, string | undefined]> = [
    ["admin", ["ADMIN", "ARCHITECT", "VIEWER"], "Ada Admin", readEnv("AUTH_LOCAL_ADMIN_PASSWORD")],
    ["architect", ["ARCHITECT", "VIEWER"], "Alex Architect", readEnv("AUTH_LOCAL_ARCHITECT_PASSWORD")],
    ["viewer", ["VIEWER"], "Vera Viewer", readEnv("AUTH_LOCAL_VIEWER_PASSWORD")],
  ];
  const users: Record<string, { password: string; roles: UserRole[]; name: string }> = {};
  for (const [username, roles, name, configured] of entries) {
    const password = resolveLocalPassword(configured);
    if (!password) {
      continue;
    }
    users[username] = { password, roles, name };
  }
  return users;
}

const keycloakConfigured = keycloakConfiguredNow();
const localAuthEnabled = isLocalAuthAllowed();

const providers = [
  ...(keycloakConfigured
    ? [
        Keycloak({
          clientId: readEnv("KEYCLOAK_CLIENT_ID") ?? "",
          clientSecret: readEnv("KEYCLOAK_CLIENT_SECRET") ?? "",
          issuer: readEnv("KEYCLOAK_ISSUER") ?? "",
        }),
      ]
    : []),
  ...(localAuthEnabled
    ? [
        Credentials({
          id: "local",
          name: "Local",
          credentials: {
            username: { label: "Usuário", type: "text" },
            password: { label: "Senha", type: "password" },
          },
          async authorize(credentials) {
            const username = String(credentials?.username ?? "");
            const password = String(credentials?.password ?? "");
            const user = loadLocalUsers()[username];
            if (!user || user.password !== password) {
              return null;
            }
            return {
              id: username,
              name: user.name,
              email: `${username}@archlens.local`,
              roles: user.roles,
            };
          },
        }),
      ]
    : []),
];

export const { handlers, auth, signIn, signOut } = NextAuth({
  secret: readEnv("AUTH_SECRET") ?? readEnv("NEXTAUTH_SECRET"),
  providers,
  pages: {
    signIn: "/login",
  },
  callbacks: {
    async jwt({ token, account, profile, user }) {
      if (account?.access_token) {
        token.accessToken = account.access_token;
      }
      if (profile) {
        token.roles = extractRolesFromProfile(
          profile as Record<string, unknown>,
        );
      }
      if (user && "roles" in user && Array.isArray(user.roles)) {
        token.roles = user.roles as UserRole[];
      }
      if (!token.roles) {
        token.roles = ["VIEWER"];
      }
      return token;
    },
    async session({ session, token }) {
      session.accessToken = token.accessToken;
      session.roles = (token.roles as UserRole[]) ?? ["VIEWER"];
      return session;
    },
  },
  trustHost: true,
});

export function isAuthConfigured(): boolean {
  return keycloakConfigured || localAuthEnabled;
}

export function isKeycloakConfigured(): boolean {
  return keycloakConfiguredNow();
}

export function isLocalAuthEnabled(): boolean {
  return isLocalAuthAllowed();
}
