import "server-only";

/**
 * Proxy serveur vers l'API Spring : MERCHANT_API_KEY ne doit jamais atteindre le
 * navigateur, donc tout appel passe par un Server Component / Server Action / Route
 * Handler qui utilise ce module.
 */

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string
  ) {
    super(message);
  }
}

function baseUrl(): string {
  const url = process.env.API_BASE_URL;
  if (!url) throw new Error("API_BASE_URL manquant");
  return url.replace(/\/$/, "");
}

function apiKey(): string {
  const key = process.env.MERCHANT_API_KEY;
  if (!key) throw new Error("MERCHANT_API_KEY manquant");
  return key;
}

async function request<T>(
  path: string,
  init?: RequestInit & { parseJson?: boolean }
): Promise<T> {
  const res = await fetch(`${baseUrl()}${path}`, {
    ...init,
    headers: {
      "X-Api-Key": apiKey(),
      ...(init?.body ? { "Content-Type": "application/json" } : {}),
      ...init?.headers,
    },
    cache: "no-store",
  });

  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new ApiError(res.status, `${init?.method ?? "GET"} ${path} -> HTTP ${res.status} ${body}`);
  }

  if (init?.parseJson === false || res.status === 204) {
    return undefined as T;
  }

  return (await res.json()) as T;
}

export function apiGet<T>(path: string): Promise<T> {
  return request<T>(path);
}

export function apiPost<T>(path: string, body?: unknown): Promise<T> {
  return request<T>(path, {
    method: "POST",
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
}

export function apiPut<T>(path: string, body: unknown): Promise<T> {
  return request<T>(path, {
    method: "PUT",
    body: JSON.stringify(body),
  });
}
