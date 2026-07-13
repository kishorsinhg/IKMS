export class ApiClientError extends Error {
  readonly status: number;
  readonly data: unknown;

  constructor(status: number, data: unknown, message?: string) {
    super(message ?? `Request failed with status ${status}`);
    this.status = status;
    this.data = data;
  }
}

class ApiClient {
  private readonly baseUrl: string;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl.replace(/\/$/, "");
  }

  async get<T>(path: string): Promise<T> {
    return this.request<T>(path, { method: "GET" });
  }

  async post<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>(path, {
      method: "POST",
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  }

  async patch<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>(path, {
      method: "PATCH",
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  }

  async delete<T>(path: string): Promise<T> {
    return this.request<T>(path, { method: "DELETE" });
  }

  async postForm<T>(path: string, body: FormData): Promise<T> {
    return this.request<T>(path, {
      method: "POST",
      body,
      headers: {},
    });
  }

  private async request<T>(path: string, init: RequestInit): Promise<T> {
    const isFormData = init.body instanceof FormData;
    const response = await fetch(`${this.baseUrl}${path}`, {
      ...init,
      credentials: "include",
      headers: {
        ...(isFormData ? {} : { "Content-Type": "application/json" }),
        ...(init.headers ?? {}),
      },
    });

    const contentType = response.headers.get("content-type") ?? "";
    const payload = contentType.includes("application/json")
      ? await response.json()
      : await response.text();

    if (!response.ok) {
      throw new ApiClientError(response.status, payload);
    }

    return payload as T;
  }
}

export const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export const apiClient = new ApiClient(apiBaseUrl);
