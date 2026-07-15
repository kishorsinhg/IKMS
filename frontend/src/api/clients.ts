import { apiClient } from "./client";
import {
  createDemoClient,
  createDemoNote,
  deleteDemoNote,
  getDemoClient,
  isDemoDataEnabled,
  listDemoClients,
  listDemoNotes,
  updateDemoNote,
} from "./demo";
export {
  getClientKnowledgeTimeline as listKnowledgeTimeline,
  getClientRelatedKnowledge as listRelatedKnowledge,
  getDocumentVersions as listDocumentVersions,
  getSourceRelatedKnowledge as listSourceRelatedKnowledge,
} from "./knowledge";
export type {
  BusinessReferenceField,
  ClientKnowledgeTimelineParams,
  CustomerKnowledgeTimelineEvent,
  CustomerKnowledgeTimelineFilters,
  CustomerKnowledgeTimelinePage,
  DocumentVersionSummary,
  KnowledgeEvidenceReference,
  KnowledgeSourceReference,
  RelatedKnowledgeLink,
  RelatedKnowledgeResponse,
} from "./knowledge";

export type ClientType = "INDIVIDUAL" | "BUSINESS";
export type ClientStatus = "ACTIVE" | "INACTIVE" | "ARCHIVED";

export interface ClientSummary {
  id: string;
  clientId: string;
  clientIdTemporary: boolean;
  clientType: ClientType;
  status: ClientStatus;
  displayName: string;
}

export interface ClientProfile extends ClientSummary {
  legalName: string | null;
  primaryEmail: string | null;
  primaryPhone: string | null;
  contactPerson: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateClientRequest {
  clientId?: string;
  clientType: ClientType;
  displayName: string;
  legalName?: string;
  primaryEmail?: string;
  primaryPhone?: string;
  contactPerson?: string;
}

export interface UpdateClientRequest extends CreateClientRequest {
  status: ClientStatus;
}

export interface Note {
  id: string;
  clientId: string;
  noteText: string;
  status: "ACTIVE" | "DELETED";
  createdAt: string;
  updatedAt: string;
}

export interface CreateNoteRequest {
  noteText: string;
}

export interface UpdateNoteRequest {
  noteText: string;
}

export interface ClientImportRowResult {
  lineNumber: number;
  clientId: string;
  displayName: string;
  email: string;
  clientType: string;
  status: string;
  warnings: string[];
  errors: string[];
  accepted: boolean;
}

export interface ClientImportResult {
  filename: string;
  totalRows: number;
  acceptedRows: number;
  warningCount: number;
  errorCount: number;
  fileErrors: string[];
  rows: ClientImportRowResult[];
}

export function importClients(file: File) {
  const formData = new FormData();
  formData.set("file", file);
  return apiClient.postForm<ClientImportResult>("/api/clients/import", formData);
}

export function listClients(query = "") {
  if (isDemoDataEnabled) {
    return listDemoClients(query);
  }
  const search = query ? `?query=${encodeURIComponent(query)}` : "";
  return apiClient.get<ClientSummary[]>(`/api/clients${search}`);
}

export function createClient(request: CreateClientRequest) {
  if (isDemoDataEnabled) {
    return createDemoClient(request);
  }
  return apiClient.post<ClientProfile>("/api/clients", request);
}

export function getClient(clientId: string) {
  if (isDemoDataEnabled) {
    return getDemoClient(clientId);
  }
  return apiClient.get<ClientProfile>(`/api/clients/${clientId}`);
}

export function updateClient(clientId: string, request: UpdateClientRequest) {
  return apiClient.patch<ClientProfile>(`/api/clients/${clientId}`, request);
}

export function listNotes(clientId: string) {
  if (isDemoDataEnabled) {
    return listDemoNotes(clientId);
  }
  return apiClient.get<Note[]>(`/api/clients/${clientId}/notes`);
}

export function createNote(clientId: string, request: CreateNoteRequest) {
  if (isDemoDataEnabled) {
    return createDemoNote(clientId, request);
  }
  return apiClient.post<Note>(`/api/clients/${clientId}/notes`, request);
}

export function updateNote(noteId: string, request: UpdateNoteRequest) {
  if (isDemoDataEnabled) {
    return updateDemoNote(noteId, request);
  }
  return apiClient.patch<Note>(`/api/notes/${noteId}`, request);
}

export function deleteNote(noteId: string) {
  if (isDemoDataEnabled) {
    return deleteDemoNote(noteId);
  }
  return apiClient.delete<void>(`/api/notes/${noteId}`);
}
