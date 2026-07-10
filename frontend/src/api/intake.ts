import { apiClient } from "./client";

export type ReviewQueueItemType = "DOCUMENT" | "EMAIL" | "DOCUMENT_VERSION";
export type ReviewQueueReason = "UNLINKED" | "LOW_CONFIDENCE" | "MISSING_METADATA" | "UNSUPPORTED";
export type ReviewQueueStatus = "OPEN" | "IN_PROGRESS" | "RESOLVED" | "REJECTED";

export interface UploadDocumentResult {
  documentId: string | null;
  versionId: string | null;
  outcome: "CREATED" | "DUPLICATE";
  reviewStatus: string;
  duplicateOfDocumentId: string | null;
}

export interface ClientDocumentSummary {
  id: string;
  clientId: string | null;
  title: string;
  source: string;
  processingStatus: string;
  reviewStatus: string;
  currentVersionId: string | null;
  parentEmailId: string | null;
  createdAt: string;
}

export interface ClientEmailSummary {
  id: string;
  clientId: string | null;
  subject: string;
  sender: string;
  recipients: string;
  processingStatus: string;
  reviewStatus: string;
  receivedAt: string;
}

export interface ReviewQueueItem {
  id: string;
  itemType: ReviewQueueItemType;
  itemId: string;
  reason: ReviewQueueReason;
  status: ReviewQueueStatus;
  assignedTo: string | null;
}

export function uploadDocument(file: File, clientId?: string) {
  const formData = new FormData();
  formData.set("file", file);
  if (clientId) {
    formData.set("clientId", clientId);
  }
  return apiClient.postForm<UploadDocumentResult>("/api/documents/upload", formData);
}

export function listClientDocuments(clientId: string) {
  return apiClient.get<ClientDocumentSummary[]>(`/api/clients/${clientId}/documents`);
}

export function listClientEmails(clientId: string) {
  return apiClient.get<ClientEmailSummary[]>(`/api/clients/${clientId}/emails`);
}

export function listReviewQueue(status?: ReviewQueueStatus | "", reason?: ReviewQueueReason | "") {
  const searchParams = new URLSearchParams();
  if (status) {
    searchParams.set("status", status);
  }
  if (reason) {
    searchParams.set("reason", reason);
  }
  const search = searchParams.toString();
  return apiClient.get<ReviewQueueItem[]>(`/api/review-queue${search ? `?${search}` : ""}`);
}

export function getReviewQueueItem(itemId: string) {
  return apiClient.get<ReviewQueueItem>(`/api/review-queue/${itemId}`);
}

export function linkReviewItemClient(itemId: string, clientId: string) {
  return apiClient.post<ReviewQueueItem>(`/api/review-queue/${itemId}/link-client`, { clientId });
}

export function correctReviewItemMetadata(itemId: string, title: string) {
  return apiClient.patch<ReviewQueueItem>(`/api/review-queue/${itemId}/metadata`, { title });
}

export function approveReviewItem(itemId: string) {
  return apiClient.post<ReviewQueueItem>(`/api/review-queue/${itemId}/approve`);
}

export function rejectReviewItem(itemId: string, reason: string) {
  return apiClient.post<ReviewQueueItem>(`/api/review-queue/${itemId}/reject`, { reason });
}
