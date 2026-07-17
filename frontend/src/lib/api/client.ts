import type { ApiError, ProblemDetails } from "@/types";

const DEFAULT_TIMEOUT_MS = 30_000;
const CORRELATION_HEADER = "X-Correlation-Id";

/**
 * Base da API.
 * No servidor (RSC/actions), preferir URL interna do Compose (`ARCHLENS_API_INTERNAL_URL`),
 * pois `localhost` dentro do container não alcança o backend. No browser, usar a URL pública.
 */
export function getApiBaseUrl(): string {
  const publicUrl =
    process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1";
  if (typeof window === "undefined") {
    return process.env.ARCHLENS_API_INTERNAL_URL ?? publicUrl;
  }
  return publicUrl;
}

export function getApiTimeoutMs(): number {
  const raw = process.env.API_TIMEOUT_MS;
  if (!raw) return DEFAULT_TIMEOUT_MS;
  const n = Number(raw);
  return Number.isFinite(n) && n > 0 ? n : DEFAULT_TIMEOUT_MS;
}

export function createCorrelationId(): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `corr-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

export class ApiClientError extends Error {
  readonly status: number;
  readonly title?: string;
  readonly detail?: string;
  readonly correlationId?: string;
  readonly type?: string;
  readonly details?: string;

  constructor(error: ApiError) {
    super(error.message);
    this.name = "ApiClientError";
    this.status = error.status;
    this.title = error.title;
    this.detail = error.detail;
    this.correlationId = error.correlationId;
    this.type = error.type;
    this.details = error.details;
  }

  get isUnauthorized(): boolean {
    return this.status === 401;
  }

  get isForbidden(): boolean {
    return this.status === 403;
  }
}

export interface DownloadResult {
  blob: Blob;
  contentType: string;
  filename?: string;
}

function parseFilename(contentDisposition: string | null): string | undefined {
  if (!contentDisposition) return undefined;
  const utf8 = /filename\*=UTF-8''([^;]+)/i.exec(contentDisposition);
  if (utf8?.[1]) {
    try {
      return decodeURIComponent(utf8[1].trim());
    } catch {
      return utf8[1].trim();
    }
  }
  const plain = /filename="?([^";]+)"?/i.exec(contentDisposition);
  return plain?.[1]?.trim();
}

export function parseProblemDetails(
  status: number,
  statusText: string,
  body: unknown,
): ApiError {
  const fallback: ApiError = {
    status,
    message: statusText || "Erro na requisição",
  };

  if (!body || typeof body !== "object") {
    return fallback;
  }

  const problem = body as ProblemDetails & {
    message?: string;
    error?: string;
  };

  const detail =
    typeof problem.detail === "string"
      ? problem.detail
      : typeof problem.message === "string"
        ? problem.message
        : typeof problem.error === "string"
          ? problem.error
          : undefined;

  const title =
    typeof problem.title === "string" ? problem.title : undefined;

  const message =
    detail ??
    title ??
    (status === 403
      ? "Você não tem permissão para esta ação."
      : status === 401
        ? "Sessão expirada ou não autenticada. Faça login novamente."
        : fallback.message);

  return {
    status: typeof problem.status === "number" ? problem.status : status,
    message,
    title,
    detail,
    correlationId:
      typeof problem.correlationId === "string"
        ? problem.correlationId
        : undefined,
    type: typeof problem.type === "string" ? problem.type : undefined,
    details: JSON.stringify(body),
  };
}

async function parseError(response: Response): Promise<ApiError> {
  const contentType = response.headers.get("content-type") ?? "";
  try {
    if (
      contentType.includes("application/problem+json") ||
      contentType.includes("application/json")
    ) {
      const body: unknown = await response.json();
      return parseProblemDetails(
        response.status,
        response.statusText,
        body,
      );
    }
    const text = await response.text();
    if (text) {
      return {
        status: response.status,
        message: text.slice(0, 500),
        details: text.slice(0, 2000),
      };
    }
  } catch {
    // corpo ilegível
  }
  return parseProblemDetails(response.status, response.statusText, null);
}

export type ApiFetchOptions = RequestInit & {
  accessToken?: string;
  correlationId?: string;
  timeoutMs?: number;
};

export async function apiFetch<T>(
  path: string,
  options: ApiFetchOptions = {},
): Promise<T> {
  const {
    accessToken,
    headers,
    correlationId,
    timeoutMs,
    signal,
    ...rest
  } = options;
  const url = `${getApiBaseUrl()}${path.startsWith("/") ? path : `/${path}`}`;
  const corr = correlationId ?? createCorrelationId();
  const timeout = timeoutMs ?? getApiTimeoutMs();
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeout);

  if (signal) {
    if (signal.aborted) {
      controller.abort();
    } else {
      signal.addEventListener("abort", () => controller.abort(), {
        once: true,
      });
    }
  }

  try {
    const response = await fetch(url, {
      ...rest,
      signal: controller.signal,
      headers: {
        Accept: "application/json, application/problem+json",
        ...(rest.body instanceof FormData
          ? {}
          : rest.body
            ? { "Content-Type": "application/json" }
            : {}),
        ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
        [CORRELATION_HEADER]: corr,
        ...headers,
      },
    });

    if (!response.ok) {
      throw new ApiClientError(await parseError(response));
    }

    if (response.status === 204) {
      return undefined as T;
    }

    const contentType = response.headers.get("content-type") ?? "";
    if (contentType.includes("application/json")) {
      return (await response.json()) as T;
    }

    const text = await response.text();
    return text as T;
  } catch (err) {
    if (err instanceof ApiClientError) {
      throw err;
    }
    if (err instanceof Error && err.name === "AbortError") {
      throw new ApiClientError({
        status: 408,
        message: "Tempo esgotado ao chamar a API.",
        correlationId: corr,
      });
    }
    throw err;
  } finally {
    clearTimeout(timer);
  }
}

export async function apiDownload(
  path: string,
  auth?: string | { accessToken?: string; user?: string; roles?: string[] },
  options: { correlationId?: string; timeoutMs?: number } = {},
): Promise<DownloadResult> {
  const url = `${getApiBaseUrl()}${path.startsWith("/") ? path : `/${path}`}`;
  const corr = options.correlationId ?? createCorrelationId();
  const headers: Record<string, string> = {
    Accept: "*/*",
    [CORRELATION_HEADER]: corr,
  };

  if (typeof auth === "string" && auth) {
    headers.Authorization = `Bearer ${auth}`;
  } else if (auth && typeof auth === "object" && auth.accessToken) {
    headers.Authorization = `Bearer ${auth.accessToken}`;
  } else if (auth && typeof auth === "object") {
    if (!auth.user) {
      throw new ApiClientError({
        status: 401,
        message: "Identidade local ausente para download.",
      });
    }
    headers["X-User"] = auth.user;
    headers["X-Roles"] = (auth.roles ?? ["VIEWER"]).join(",");
  } else {
    throw new ApiClientError({
      status: 401,
      message: "Autenticação necessária para download.",
    });
  }

  const controller = new AbortController();
  const timer = setTimeout(
    () => controller.abort(),
    options.timeoutMs ?? getApiTimeoutMs(),
  );

  try {
    const response = await fetch(url, {
      headers,
      signal: controller.signal,
    });

    if (!response.ok) {
      throw new ApiClientError(await parseError(response));
    }

    const contentType =
      response.headers.get("content-type") ?? "application/octet-stream";
    const filename = parseFilename(
      response.headers.get("content-disposition"),
    );
    const blob = await response.blob();
    return { blob, contentType, filename };
  } catch (err) {
    if (err instanceof ApiClientError) throw err;
    if (err instanceof Error && err.name === "AbortError") {
      throw new ApiClientError({
        status: 408,
        message: "Tempo esgotado no download.",
        correlationId: corr,
      });
    }
    throw err;
  } finally {
    clearTimeout(timer);
  }
}
