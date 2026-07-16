import type { ApiError } from "@/types";

export function getApiBaseUrl(): string {
  return process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1";
}

export class ApiClientError extends Error {
  readonly status: number;
  readonly details?: string;

  constructor(error: ApiError) {
    super(error.message);
    this.name = "ApiClientError";
    this.status = error.status;
    this.details = error.details;
  }
}

async function parseError(response: Response): Promise<ApiError> {
  let message = response.statusText || "Erro na requisição";
  let details: string | undefined;

  try {
    const body = (await response.json()) as { message?: string; error?: string };
    message = body.message ?? body.error ?? message;
    details = JSON.stringify(body);
  } catch {
    // response body is not JSON
  }

  return { status: response.status, message, details };
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit & { accessToken?: string } = {},
): Promise<T> {
  const { accessToken, headers, ...rest } = options;
  const url = `${getApiBaseUrl()}${path.startsWith("/") ? path : `/${path}`}`;

  const response = await fetch(url, {
    ...rest,
    headers: {
      Accept: "application/json",
      ...(rest.body instanceof FormData
        ? {}
        : { "Content-Type": "application/json" }),
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
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

  return (await response.text()) as T;
}

export async function apiDownload(
  path: string,
  auth?: string | { accessToken?: string; user?: string; roles?: string[] },
): Promise<Blob> {
  const url = `${getApiBaseUrl()}${path.startsWith("/") ? path : `/${path}`}`;
  let headers: Record<string, string> = {};
  if (typeof auth === "string" && auth) {
    headers = { Authorization: `Bearer ${auth}` };
  } else if (auth && typeof auth === "object" && auth.accessToken) {
    headers = { Authorization: `Bearer ${auth.accessToken}` };
  } else {
    const user =
      (typeof auth === "object" && auth?.user) ||
      process.env.NEXT_PUBLIC_DEV_USER ||
      "architect";
    const roles =
      (typeof auth === "object" && auth?.roles?.join(",")) ||
      process.env.NEXT_PUBLIC_DEV_ROLES ||
      "ARCHITECT,VIEWER,ADMIN";
    headers = { "X-User": user, "X-Roles": roles };
  }
  const response = await fetch(url, { headers });

  if (!response.ok) {
    throw new ApiClientError(await parseError(response));
  }

  return response.blob();
}
