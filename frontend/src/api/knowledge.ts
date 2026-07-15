import { apiClient } from "./client";

export interface BusinessReferenceField {
  key: string;
  label: string;
  value: string;
}

export interface KnowledgeEvidenceReference {
  sourceType: string;
  sourceId: string;
  sourceVersionId: string | null;
  title: string;
  detail: string;
  pageNumber: number | null;
  section: string | null;
  jumpTargetId: string;
}

export interface CustomerKnowledgeTimelineEvent {
  eventId: string;
  customerId: string;
  eventType: string;
  sourceType: string;
  sourceId: string;
  sourceVersionId: string | null;
  title: string;
  summary: string;
  occurredAt: string;
  recordedAt: string;
  actor: string | null;
  documentType: string | null;
  businessReferenceFields: BusinessReferenceField[];
  status: string;
  evidenceReferences: KnowledgeEvidenceReference[];
  availableActions: string[];
  permissionState: string;
  correlationId: string | null;
}

export interface CustomerKnowledgeTimelineFilters {
  query: string | null;
  from: string | null;
  to: string | null;
  sourceType: string | null;
  eventType: string | null;
  documentType: string | null;
  reviewStatus: string | null;
  policyNumber: string | null;
  claimNumber: string | null;
  insurer: string | null;
  actor: string | null;
  sortDirection: string;
  limit: number;
}

export interface CustomerKnowledgeTimelinePage {
  events: CustomerKnowledgeTimelineEvent[];
  nextCursor: string | null;
  hasMore: boolean;
  appliedFilters: CustomerKnowledgeTimelineFilters;
}

export interface RelatedKnowledgeLink {
  relationshipId: string;
  customerId: string;
  sourceType: string;
  sourceId: string;
  sourceTitle: string;
  relatedSourceType: string;
  relatedSourceId: string;
  relatedTitle: string;
  relationshipType: string;
  score: number | null;
  explanation: string;
  supportingFields: Record<string, string>;
  evidenceReferences: KnowledgeEvidenceReference[];
  derivationType: string;
  createdAt: string;
  inferred: boolean;
}

export interface RelatedKnowledgeResponse {
  customerId: string;
  sourceType: string;
  sourceId: string;
  links: RelatedKnowledgeLink[];
  restrictedContentNotice: string | null;
}

export interface DocumentVersionSummary {
  id: string;
  documentId: string;
  versionNumber: number;
  fileName: string;
  mimeType: string;
  redactionStatus: string;
  current: boolean;
  fileHash: string;
  createdAt: string;
  createdBy: string | null;
}

export interface KnowledgeSourceReference {
  clientId: string;
  sourceType: "CUSTOMER" | "DOCUMENT" | "EMAIL" | "NOTE" | "REVIEW";
  sourceId: string;
}

export interface ClientKnowledgeTimelineParams {
  cursor?: string;
  limit?: number;
  query?: string;
  from?: string;
  to?: string;
  sourceType?: string;
  eventType?: string;
  documentType?: string;
  reviewStatus?: string;
  policyNumber?: string;
  claimNumber?: string;
  insurer?: string;
  actor?: string;
}

export const knowledgeQueryKeys = {
  clientTimeline: (clientId: string, params?: ClientKnowledgeTimelineParams) =>
    ["clients", clientId, "knowledge", "timeline", params ?? {}] as const,
  clientRelated: (clientId: string, limit = 20) =>
    ["clients", clientId, "knowledge", "related", { limit }] as const,
  sourceRelated: (sourceType: string, sourceId: string, limit = 12) =>
    ["knowledge", "sources", sourceType, sourceId, "related", { limit }] as const,
  documentVersions: (documentId: string) =>
    ["documents", documentId, "versions"] as const,
};

export function getClientKnowledgeTimeline(
  clientId: string,
  params?: ClientKnowledgeTimelineParams,
  signal?: AbortSignal,
) {
  const searchParams = new URLSearchParams();
  Object.entries(params ?? {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && `${value}`.trim() !== "") {
      searchParams.set(key, String(value));
    }
  });
  const search = searchParams.toString();
  return apiClient.get<CustomerKnowledgeTimelinePage>(
    `/api/clients/${clientId}/knowledge/timeline${search ? `?${search}` : ""}`,
    { signal },
  );
}

export function getClientRelatedKnowledge(clientId: string, limit = 20, signal?: AbortSignal) {
  return apiClient.get<RelatedKnowledgeResponse>(
    `/api/clients/${clientId}/knowledge/related?limit=${limit}`,
    { signal },
  );
}

export function getSourceRelatedKnowledge(
  sourceType: string,
  sourceId: string,
  limit = 12,
  signal?: AbortSignal,
) {
  return apiClient.get<RelatedKnowledgeResponse>(
    `/api/knowledge/sources/${sourceType}/${sourceId}/related?limit=${limit}`,
    { signal },
  );
}

export function getDocumentVersions(documentId: string, signal?: AbortSignal) {
  return apiClient.get<DocumentVersionSummary[]>(`/api/documents/${documentId}/versions`, { signal });
}
