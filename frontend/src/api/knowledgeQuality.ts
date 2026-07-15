import { apiClient } from "./client";

export interface QualityDimensionScore {
  key: string;
  label: string;
  score: number;
  summary: string;
}

export interface KnowledgeQualityIssue {
  id: string;
  clientId: string;
  sourceType: string | null;
  sourceId: string | null;
  category: string;
  issueType: string;
  severity: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  status: "OPEN" | "RESOLVED" | "DISMISSED";
  title: string;
  detail: string | null;
  recommendationType: string | null;
  recommendationDetail: string | null;
  businessReferenceKey: string | null;
  scoreImpact: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface CustomerKnowledgeQualitySummary {
  clientId: string;
  customerName: string;
  customerExternalId: string;
  overallScore: number;
  readinessState: "READY" | "NEEDS_ATTENTION" | "BLOCKED";
  issueCount: number;
  openIssueCount: number;
  evaluatedAt: string;
  dimensions: QualityDimensionScore[];
  recommendationHighlights: string[];
}

export interface CustomerKnowledgeQualityDetail {
  summary: CustomerKnowledgeQualitySummary;
  issues: KnowledgeQualityIssue[];
}

export interface KnowledgeQualityCustomerListResponse {
  customers: CustomerKnowledgeQualitySummary[];
}

export interface KnowledgeQualityIssueQueueResponse {
  issues: KnowledgeQualityIssue[];
}

export interface KnowledgeQualityActionResult {
  action: string;
  affectedCustomers: number;
  affectedItems: number;
  clientIds: string[];
  completedAt: string;
}

export interface QualityRevalidateRequest {
  clientIds: string[];
  confirmed: boolean;
}

export interface QualityReindexRequest {
  clientIds: string[];
  confirmed: boolean;
}

export interface BulkQualityCorrectionItemRequest {
  clientId: string;
  sourceType?: string;
  sourceId?: string;
  fieldKey?: string;
  value?: string;
  targetClientId?: string;
}

export interface BulkQualityCorrectionRequest {
  operationType:
    | "METADATA_CORRECTION"
    | "BUSINESS_REFERENCE_CORRECTION"
    | "CUSTOMER_REASSIGNMENT"
    | "PUBLISH";
  confirmed: boolean;
  items: BulkQualityCorrectionItemRequest[];
}

export const knowledgeQualityQueryKeys = {
  customers: (query = "") => ["knowledge-quality", "customers", query] as const,
  customer: (clientId: string) => ["knowledge-quality", "customer", clientId] as const,
  issues: (clientId?: string) => ["knowledge-quality", "issues", clientId ?? "all"] as const,
};

export function listKnowledgeQualityCustomers(query = "", refresh = false, signal?: AbortSignal) {
  const search = new URLSearchParams();
  if (query.trim()) {
    search.set("query", query.trim());
  }
  if (refresh) {
    search.set("refresh", "true");
  }
  const suffix = search.toString();
  return apiClient.get<KnowledgeQualityCustomerListResponse>(
    `/api/knowledge-quality/customers${suffix ? `?${suffix}` : ""}`,
    { signal },
  );
}

export function getKnowledgeQualityCustomer(clientId: string, refresh = false, signal?: AbortSignal) {
  const search = refresh ? "?refresh=true" : "";
  return apiClient.get<CustomerKnowledgeQualityDetail>(
    `/api/knowledge-quality/customer/${clientId}${search}`,
    { signal },
  );
}

export function listKnowledgeQualityIssues(clientId?: string, signal?: AbortSignal) {
  const search = clientId ? `?clientId=${clientId}` : "";
  return apiClient.get<KnowledgeQualityIssueQueueResponse>(`/api/knowledge-quality/issues${search}`, { signal });
}

export function revalidateKnowledgeQuality(request: QualityRevalidateRequest) {
  return apiClient.post<KnowledgeQualityActionResult>("/api/knowledge-quality/revalidate", request);
}

export function reindexKnowledgeQuality(request: QualityReindexRequest) {
  return apiClient.post<KnowledgeQualityActionResult>("/api/knowledge-quality/reindex", request);
}

export function bulkCorrectKnowledgeQuality(request: BulkQualityCorrectionRequest) {
  return apiClient.post<KnowledgeQualityActionResult>("/api/knowledge-quality/bulk-correct", request);
}
