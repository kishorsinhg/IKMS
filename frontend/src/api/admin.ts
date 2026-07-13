import { apiClient } from "./client";
import { UserRole, UserStatus } from "./auth";

export interface AdminUser {
  id: string;
  username: string;
  displayName: string;
  email: string | null;
  status: UserStatus;
  roles: UserRole[];
}

export interface DocumentTypeConfig {
  id: string;
  name: string;
  description: string | null;
  active: boolean;
  createdAt: string;
}

export interface MetadataFieldConfig {
  id: string;
  fieldKey: string;
  label: string;
  pii: boolean;
  active: boolean;
  createdAt: string;
}

export interface SharedFolderConfig {
  id: string;
  path: string;
  active: boolean;
  createdAt: string;
}

export interface MailboxConfig {
  id: string;
  name: string;
  host: string;
  username: string;
  active: boolean;
  createdAt: string;
}

export interface ReviewSettingConfig {
  id: string;
  mode: string;
  lowConfidenceThreshold: number;
  updatedAt: string;
}

export interface AiProviderSettingConfig {
  id: string;
  providerName: string;
  modelName: string;
  apiBaseUrl: string | null;
  apiKeyConfigured: boolean;
  ocrProvider: string;
  active: boolean;
  updatedAt: string;
}

export function listAdminUsers() {
  return apiClient.get<AdminUser[]>("/api/admin/users");
}

export function listDocumentTypes() {
  return apiClient.get<DocumentTypeConfig[]>("/api/admin/document-types");
}

export function createDocumentType(request: { name: string; description?: string; active: boolean }) {
  return apiClient.post<DocumentTypeConfig>("/api/admin/document-types", request);
}

export function listMetadataFields() {
  return apiClient.get<MetadataFieldConfig[]>("/api/admin/metadata-fields");
}

export function createMetadataField(request: { fieldKey: string; label: string; pii: boolean; active: boolean }) {
  return apiClient.post<MetadataFieldConfig>("/api/admin/metadata-fields", request);
}

export function listSharedFolders() {
  return apiClient.get<SharedFolderConfig[]>("/api/admin/intake/shared-folders");
}

export function createSharedFolder(request: { path: string; active: boolean }) {
  return apiClient.post<SharedFolderConfig>("/api/admin/intake/shared-folders", request);
}

export function listMailboxes() {
  return apiClient.get<MailboxConfig[]>("/api/admin/intake/mailboxes");
}

export function createMailbox(request: { name: string; host: string; username: string; active: boolean }) {
  return apiClient.post<MailboxConfig>("/api/admin/intake/mailboxes", request);
}

export function getReviewSetting() {
  return apiClient.get<ReviewSettingConfig>("/api/admin/review-settings");
}

export function updateReviewSetting(request: { mode: string; lowConfidenceThreshold: number }) {
  return apiClient.patch<ReviewSettingConfig>("/api/admin/review-settings", request);
}

export function getAiSetting() {
  return apiClient.get<AiProviderSettingConfig>("/api/admin/ai-settings");
}

export function updateAiSetting(request: {
  providerName: string;
  modelName: string;
  apiBaseUrl: string;
  apiKey: string;
  ocrProvider: string;
  active: boolean;
}) {
  return apiClient.patch<AiProviderSettingConfig>("/api/admin/ai-settings", request);
}
