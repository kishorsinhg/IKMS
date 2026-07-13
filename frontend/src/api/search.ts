import { apiClient } from "./client";

export interface SearchResult {
  sourceType: string;
  sourceId: string;
  title: string;
  excerpt: string;
  citation: string;
  pageNumber: number | null;
  sourceSection: string | null;
  occurredAt: string;
}

export interface SourceCitation {
  sourceType: string;
  sourceId: string;
  title: string;
  excerpt: string;
  pageNumber: number | null;
  sourceSection: string | null;
}

export interface AskClientResponse {
  interactionId: string;
  status: "Answered" | "NoEvidence" | "Refused" | "Failed";
  answer: string;
  citations: SourceCitation[];
  createdAt: string;
}

export function searchClientKnowledge(clientId: string, query: string) {
  const search = query ? `?query=${encodeURIComponent(query)}` : "";
  return apiClient.get<SearchResult[]>(`/api/clients/${clientId}/search${search}`);
}

export function askClientQuestion(clientId: string, question: string) {
  return apiClient.post<AskClientResponse>(`/api/clients/${clientId}/ask`, { question });
}

export function sendAiFeedback(interactionId: string, helpful: boolean, comment?: string) {
  return apiClient.post<void>(`/api/ai-interactions/${interactionId}/feedback`, { helpful, comment });
}
