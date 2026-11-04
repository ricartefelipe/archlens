import { auth } from "@/auth";
import {
  crossSiteWrite,
  forwardedRequestHeaders,
  forwardedResponseHeaders,
  identityFromSession,
  problem,
  upstreamPath,
} from "@/lib/api/bff";
import { getApiTimeoutMs } from "@/lib/api/client";

export const dynamic = "force-dynamic";

function apiBaseUrl(): string {
  return (
    process.env.ARCHLENS_API_INTERNAL_URL ??
    process.env.NEXT_PUBLIC_API_BASE_URL ??
    "http://localhost:8080/api/v1"
  ).replace(/\/+$/, "");
}

async function handle(
  request: Request,
  context: { params: Promise<{ path: string[] }> },
): Promise<Response> {
  if (crossSiteWrite(request.method, request.headers.get("sec-fetch-site"))) {
    return problem(403, "Origem não permitida", "Escrita só a partir do próprio ArchLens.");
  }

  const { path } = await context.params;
  const apiPath = upstreamPath(path ?? []);
  if (!apiPath) {
    return problem(404, "Não encontrado", "Rota da API inválida.");
  }

  const identity = identityFromSession(await auth(), process.env.ARCHLENS_LOCAL_HEADER_AUTH_SECRET);
  if (!identity.ok) {
    return problem(identity.status, identity.status === 401 ? "Não autenticado" : "Configuração inválida", identity.detail);
  }

  const search = new URL(request.url).search;
  const method = request.method.toUpperCase();
  let body: ArrayBuffer | undefined;
  if (method !== "GET" && method !== "HEAD") {
    const raw = await request.arrayBuffer();
    body = raw.byteLength > 0 ? raw : undefined;
  }

  let upstream: Response;
  try {
    upstream = await fetch(`${apiBaseUrl()}${apiPath}${search}`, {
      method,
      headers: { ...forwardedRequestHeaders(request.headers), ...identity.headers },
      body,
      cache: "no-store",
      redirect: "manual",
      signal: AbortSignal.timeout(getApiTimeoutMs()),
    });
  } catch (err) {
    const timedOut = err instanceof Error && (err.name === "TimeoutError" || err.name === "AbortError");
    return problem(timedOut ? 504 : 502, "API indisponível", timedOut ? "Tempo esgotado ao chamar a API." : "Não foi possível falar com a API.");
  }

  const noBody = upstream.status === 204 || upstream.status === 304 || method === "HEAD";
  return new Response(noBody ? null : upstream.body, {
    status: upstream.status,
    headers: forwardedResponseHeaders(upstream.headers),
  });
}

export { handle as GET, handle as POST, handle as PUT, handle as PATCH, handle as DELETE, handle as HEAD };
