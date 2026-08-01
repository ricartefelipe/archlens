import NextAuth from "next-auth";
import Credentials from "next-auth/providers/credentials";
import Keycloak from "next-auth/providers/keycloak";
import { extractRolesFromProfile } from "@/lib/auth/roles";
import type { UserRole } from "@/types";

const keycloakConfigured =
  Boolean(process.env.KEYCLOAK_CLIENT_ID) &&
  Boolean(process.env.KEYCLOAK_ISSUER);

/** Login local somente quando explicitamente habilitado (dev/teste). */
const localAuthEnabled = process.env.AUTH_LOCAL_ENABLED === "true";

function loadLocalUsers(): Record<
  string,
  { password: string; roles: UserRole[]; name: string }
> {
  if (!localAuthEnabled) {
    return {};
  }
  // Credenciais apenas em runtime server-side; nunca embutidas no client bundle.
  return {
    admin: {
      password: process.env.AUTH_LOCAL_ADMIN_PASSWORD ?? "admin123",
      roles: ["ADMIN", "ARCHITECT", "VIEWER"],
      name: "Ada Admin",
    },
    architect: {
      password: process.env.AUTH_LOCAL_ARCHITECT_PASSWORD ?? "architect123",
      roles: ["ARCHITECT", "VIEWER"],
      name: "Alex Architect",
    },
    viewer: {
      password: process.env.AUTH_LOCAL_VIEWER_PASSWORD ?? "viewer123",
      roles: ["VIEWER"],
      name: "Vera Viewer",
    },
  };
}

const providers = [
  ...(keycloakConfigured
    ? [
        Keycloak({
          clientId: process.env.KEYCLOAK_CLIENT_ID!,
          clientSecret: process.env.KEYCLOAK_CLIENT_SECRET ?? "",
          issuer: process.env.KEYCLOAK_ISSUER!,
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
            if (user && user.password === password) {
              return {
                id: username,
                name: user.name,
                email: `${username}@archlens.local`,
                roles: user.roles,
              };
            }
            return null;
          },
        }),
      ]
    : []),
];

export const { handlers, auth, signIn, signOut } = NextAuth({
  secret: process.env.AUTH_SECRET ?? process.env.NEXTAUTH_SECRET,
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
  return keycloakConfigured;
}

export function isLocalAuthEnabled(): boolean {
  return localAuthEnabled;
}
