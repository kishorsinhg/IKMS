import { apiBaseUrl, apiClient } from "./client";

export interface AuditLogEntry {
  id: string;
  occurredAt: string;
  retainedUntil: string;
  actorUserId: string | null;
  actorUsername: string | null;
  clientId: string | null;
  category: string;
  action: string;
  outcome: string;
  targetType: string | null;
  targetId: string | null;
  piiAccess: boolean;
  details: Record<string, string>;
}

export interface AuditFilters {
  actor?: string;
  action?: string;
  clientId?: string;
  from?: string;
  to?: string;
}

export function searchAuditLogs(filters: AuditFilters) {
  return apiClient.get<AuditLogEntry[]>(`/api/audit${toQueryString(filters)}`);
}

export async function exportAuditLogs(filters: AuditFilters) {
  const response = await fetch(`${apiBaseUrl}/api/audit/export${toQueryString(filters)}`, {
    method: "GET",
    credentials: "include",
  });
  const payload = await response.text();
  if (!response.ok) {
    throw new Error(payload || `Export failed with status ${response.status}`);
  }
  return payload;
}

function toQueryString(filters: AuditFilters) {
  const params = new URLSearchParams();
  if (filters.actor) {
    params.set("actor", filters.actor);
  }
  if (filters.action) {
    params.set("action", filters.action);
  }
  if (filters.clientId) {
    params.set("clientId", filters.clientId);
  }
  if (filters.from) {
    params.set("from", filters.from);
  }
  if (filters.to) {
    params.set("to", filters.to);
  }
  const query = params.toString();
  return query ? `?${query}` : "";
}
