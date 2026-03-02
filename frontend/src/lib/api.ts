const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export async function fetchApi<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });

  if (!res.ok) {
    const error = await res.json().catch(() => ({}));
    throw new Error((error as any)?.error?.message || `API error: ${res.status}`);
  }

  return res.json();
}

export const api = {
  get: <T>(path: string) => fetchApi<T>(path),
  post: <T>(path: string, data?: unknown) =>
    fetchApi<T>(path, { method: "POST", body: JSON.stringify(data) }),
  put: <T>(path: string, data?: unknown) =>
    fetchApi<T>(path, { method: "PUT", body: JSON.stringify(data) }),
  delete: <T>(path: string) => fetchApi<T>(path, { method: "DELETE" }),
};
