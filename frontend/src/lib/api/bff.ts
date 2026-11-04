/**
 * Ponte entre o navegador e a API (`/bff/v1/*`).
 *
 * O navegador nunca fala direto com a API nem monta headers de identidade: ele chama o próprio
 * frontend, que confere a sessão e repassa a identidade ao backend. No login local, a identidade
 * vai em `X-User`/`X-Roles` junto com o segredo compartilhado (`X-ArchLens-Proxy-Secret`), que só o
 * servidor conhece; com OIDC, vai o access token da sessão.
 */

export const PROXY_SECRET_HEADER = "X-ArchLens-Proxy-Secret";

/** Prefixo público da ponte, usado pelo navegador no lugar da URL da API. */
export const BFF_BASE_PATH = "/bff/v1";

/** Headers do navegador que seguem para a API. Identidade e autorização nunca vêm do navegador. */
const FORWARDED_REQUEST_HEADERS = [
  "accept",
  "content-type",
  "idempotency-key",
  "x-correlation-id",
] as const;

/** Headers da API que voltam ao navegador. */
const FORWARDED_RESPONSE_HEADERS = [
  "content-type",
  "content-disposition",
  "content-length",
  "cache-control",
  "etag",
  "x-correlation-id",
] as const;

/** Headers de identidade que o navegador não deve enviar (a ponte os descarta). */
export const IDENTITY_HEADERS = ["x-user", "x-roles", "x-organization-id", "authorization"] as const;

export type BffSession = {
  accessToken?: string;
  authProvider?: "local" | "oidc";
  roles?: string[];
  user?: { email?: string | null; name?: string | null } | null;
  error?: string;
} | null;

/**
 * Converte os segmentos da rota em caminho da API. Recusa segmentos vazios, `.`/`..` e barras ou
 * contrabarras embutidas, para que a ponte não alcance nada fora de `/api/v1`.
 */
export function upstreamPath(segments: readonly string[]): string | null {
  if (segments.length === 0) return null;
  for (const segment of segments) {
    if (!segment || segment === "." || segment === ".." || /[\\/]/.test(segment)) {
      return null;
    }
  }
  return `/${segments.map((s) => encodeURIComponent(s)).join("/")}`;
}

export type IdentityResult =
  | { ok: true; headers: Record<string, string> }
  | { ok: false; status: 401 | 500; detail: string };

/** Identidade que a ponte apresenta à API, sempre derivada da sessão do servidor. */
export function identityFromSession(session: BffSession, proxySecret: string | undefined): IdentityResult {
  if (!session || session.error) {
    return { ok: false, status: 401, detail: "Sessão ausente ou expirada. Faça login novamente." };
  }
  if (session.accessToken) {
    return { ok: true, headers: { Authorization: `Bearer ${session.accessToken}` } };
  }
  if (session.authProvider === "local") {
    const user = session.user?.email?.split("@")[0] ?? session.user?.name ?? undefined;
    if (!user) {
      return { ok: false, status: 401, detail: "Sessão local sem usuário. Faça login novamente." };
    }
    if (!proxySecret) {
      return {
        ok: false,
        status: 500,
        detail: "ARCHLENS_LOCAL_HEADER_AUTH_SECRET não está configurado no frontend.",
      };
    }
    return {
      ok: true,
      headers: {
        "X-User": user,
        "X-Roles": (session.roles?.length ? session.roles : ["VIEWER"]).join(","),
        [PROXY_SECRET_HEADER]: proxySecret,
      },
    };
  }
  return { ok: false, status: 401, detail: "Sessão sem token OIDC nem identidade local." };
}

export function forwardedRequestHeaders(incoming: Headers): Record<string, string> {
  const out: Record<string, string> = {};
  for (const name of FORWARDED_REQUEST_HEADERS) {
    const value = incoming.get(name);
    if (value) out[name] = value;
  }
  return out;
}

export function forwardedResponseHeaders(upstream: Headers): Headers {
  const out = new Headers();
  for (const name of FORWARDED_RESPONSE_HEADERS) {
    const value = upstream.get(name);
    if (value) out.set(name, value);
  }
  return out;
}

/**
 * Escritas só de mesma origem: o cookie de sessão já é SameSite=Lax, e o `Sec-Fetch-Site` enviado
 * pelos navegadores fecha o resto (formulário ou fetch de outro site).
 */
export function crossSiteWrite(method: string, secFetchSite: string | null): boolean {
  if (["GET", "HEAD", "OPTIONS"].includes(method.toUpperCase())) return false;
  return secFetchSite === "cross-site";
}

export function problem(status: number, title: string, detail: string): Response {
  return new Response(JSON.stringify({ type: "about:blank", title, status, detail }), {
    status,
    headers: { "content-type": "application/problem+json" },
  });
}
