import { apiClient } from "./client";
import {
  demoApproveReviewItem,
  demoCorrectReviewItemMetadata,
  demoLinkReviewItemClient,
  demoRejectReviewItem,
  getDemoReviewQueueItem,
  isDemoDataEnabled,
  listDemoClientDocuments,
  listDemoClientEmails,
  listDemoReviewQueue,
} from "./demo";

export type ReviewQueueItemType = "DOCUMENT" | "EMAIL" | "DOCUMENT_VERSION";
export type ReviewQueueReason =
  | "UNLINKED"
  | "LOW_CLIENT_CONFIDENCE"
  | "LOW_CLASSIFICATION_CONFIDENCE"
  | "LOW_EXTRACTION_CONFIDENCE"
  | "DUPLICATE_UNCERTAINTY"
  | "REDACTION_FAILED"
  | "PROMPT_INJECTION_RISK"
  | "PROCESSING_FAILED";
export type ReviewQueueStatus = "OPEN" | "IN_PROGRESS" | "RESOLVED" | "REJECTED";

export interface ReviewProcessingField {
  fieldKey: string;
  fieldLabel: string;
  fieldType: string;
  businessReferenceType: string | null;
  extractedValue: string | null;
  correctedValue: string | null;
  approvedValue: string | null;
  confidence: number | null;
  sourceType: string;
  extractionMethod: string;
  sourcePage: number | null;
  required: boolean;
  validationState: string;
}

export interface ReviewProcessingFinding {
  findingCode: string;
  severity: string;
  stage: string;
  fieldKey: string | null;
  message: string;
  evidenceText: string | null;
  sourcePage: number | null;
  confidence: number | null;
  status: string;
  resolutionComment: string | null;
  createdAt: string;
  resolvedAt: string | null;
}

export interface ReviewProcessingJob {
  id: string;
  status: string;
  currentStage: string;
  retryCount: number;
  overallConfidence: number | null;
  ocrConfidence: number | null;
  classificationConfidence: number | null;
  metadataConfidence: number | null;
  businessReferenceConfidence: number | null;
  validationConfidence: number | null;
  duplicateConfidence: number | null;
  language: string | null;
  ocrProvider: string | null;
  classificationProvider: string | null;
  lastErrorCode: string | null;
  lastErrorMessage: string | null;
  reviewerComment: string | null;
  startedAt: string | null;
  reviewRequestedAt: string | null;
  approvedAt: string | null;
  rejectedAt: string | null;
  publishedAt: string | null;
  completedAt: string | null;
  fields: ReviewProcessingField[];
  findings: ReviewProcessingFinding[];
}

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
  redactionStatus: "NOT_NEEDED" | "PENDING" | "AVAILABLE" | "FAILED" | "BLOCKED";
  containsPii: boolean;
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
  title: string | null;
  clientId: string | null;
  documentTypeId: string | null;
  metadataValues: Record<string, string>;
  processingJob?: ReviewProcessingJob | null;
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
  if (isDemoDataEnabled) {
    return listDemoClientDocuments(clientId);
  }
  return apiClient.get<ClientDocumentSummary[]>(`/api/clients/${clientId}/documents`);
}

export function listClientEmails(clientId: string) {
  if (isDemoDataEnabled) {
    return listDemoClientEmails(clientId);
  }
  return apiClient.get<ClientEmailSummary[]>(`/api/clients/${clientId}/emails`);
}

export function listReviewQueue(status?: ReviewQueueStatus | "", reason?: ReviewQueueReason | "") {
  if (isDemoDataEnabled) {
    return listDemoReviewQueue(status, reason);
  }
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
  if (isDemoDataEnabled) {
    return getDemoReviewQueueItem(itemId);
  }
  return apiClient.get<ReviewQueueItem>(`/api/review-queue/${itemId}`);
}

export function linkReviewItemClient(itemId: string, clientId: string) {
  if (isDemoDataEnabled) {
    return demoLinkReviewItemClient(itemId, clientId);
  }
  return apiClient.post<ReviewQueueItem>(`/api/review-queue/${itemId}/link-client`, { clientId });
}

export function correctReviewItemMetadata(
  itemId: string,
  request: { title: string; documentTypeId?: string; metadataValues?: Record<string, string>; reviewerComment?: string },
) {
  if (isDemoDataEnabled) {
    return demoCorrectReviewItemMetadata(itemId, request);
  }
  return apiClient.patch<ReviewQueueItem>(`/api/review-queue/${itemId}/metadata`, request);
}

export function approveReviewItem(itemId: string) {
  if (isDemoDataEnabled) {
    return demoApproveReviewItem(itemId);
  }
  return apiClient.post<ReviewQueueItem>(`/api/review-queue/${itemId}/approve`);
}

export function rejectReviewItem(itemId: string, reason: string) {
  if (isDemoDataEnabled) {
    return demoRejectReviewItem(itemId, reason);
  }
  return apiClient.post<ReviewQueueItem>(`/api/review-queue/${itemId}/reject`, { reason });
}

export function retryReviewItem(itemId: string, reviewerComment?: string) {
  return apiClient.post<ReviewQueueItem>(`/api/review-queue/${itemId}/retry`, { reviewerComment });
}
