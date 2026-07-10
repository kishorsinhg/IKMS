import { apiClient } from "./client";

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
