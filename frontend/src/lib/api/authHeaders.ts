export type AuthContext =
  | string
  | {
      accessToken?: string;
      user?: string;
      roles?: string[];
      organizationId?: string;
    }
  | undefined;

/**
 * Monta headers de autenticação a partir da sessão.
 * Sem fallback silencioso para papéis elevados — identidade deve vir da sessão.
 */
export function authHeaders(auth?: AuthContext): {
  accessToken?: string;
  headers?: Record<string, string>;
} {
  if (typeof auth === "string") {
    if (!auth) {
      throw new Error("Token de acesso ausente.");
    }
    return { accessToken: auth };
  }

  if (auth?.accessToken) {
    return { accessToken: auth.accessToken };
  }

  if (auth?.user) {
    return {
      headers: {
        "X-User": auth.user,
        "X-Roles": (auth.roles?.length ? auth.roles : ["VIEWER"]).join(","),
        ...(auth.organizationId
          ? { "X-Organization-Id": auth.organizationId }
          : {}),
      },
    };
  }

  throw new Error(
    "Sessão sem token OIDC nem identidade local. Faça login novamente.",
  );
}
