import { apiClient, ApiClientError } from "./client";
import { demoLogin, demoLogout, getDemoCurrentUser, isDemoDataEnabled } from "./demo";

export type UserStatus = "ACTIVE" | "LOCKED" | "DISABLED";

export type Permission =
  | "CLIENT_VIEW"
  | "REVIEW_QUEUE_ACCESS"
  | "INTAKE_ACCESS"
  | "SEARCH_CLIENT_KNOWLEDGE"
  | "ASK_CLIENT_AI"
  | "VIEW_REDACTED_DOCUMENTS"
  | "VIEW_ORIGINAL_DOCUMENTS"
  | "VIEW_PII"
  | "VIEW_AUDIT"
  | "EXPORT_AUDIT"
  | "EXPORT_SENSITIVE_CONTENT"
  | "MANAGE_CONFIGURATION"
  | "MANAGE_USERS"
  | "MANAGE_GOVERNANCE"
  | "VIEW_OPERATIONS"
  | "MANAGE_OPERATIONS"
  | "MANAGE_JOBS"
  | "MANAGE_REINDEX"
  | "MANAGE_AI"
  | "MANAGE_EMBEDDINGS"
  | "VIEW_HEALTH";

export type UserRole = "INDEXER" | "PROCESSOR" | "SUPERVISOR" | "ADMINISTRATOR";

export interface CurrentUser {
  id: string;
  username: string;
  displayName: string;
  email: string | null;
  status: UserStatus;
  roles: UserRole[];
  permissions: Permission[];
}

export interface LoginRequest {
  username: string;
  password: string;
}

export function getCurrentUser() {
  if (isDemoDataEnabled) {
    return getDemoCurrentUser().catch((error: { status?: number; data?: unknown; message?: string }) => {
      throw new ApiClientError(error.status ?? 500, error.data ?? null, error.message);
    });
  }
  return apiClient.get<CurrentUser>("/api/auth/me");
}

export function login(request: LoginRequest) {
  if (isDemoDataEnabled) {
    return demoLogin(request);
  }
  return apiClient.post<CurrentUser>("/api/auth/login", request);
}

export function logout() {
  if (isDemoDataEnabled) {
    return demoLogout();
  }
  return apiClient.post<void>("/api/auth/logout");
}
