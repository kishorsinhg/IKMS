import { apiClient } from "./client";
import { askDemoClientQuestion, isDemoDataEnabled, searchDemoClientKnowledge, sendDemoAiFeedback } from "./demo";

export interface SearchResult {
  sourceType: string;
  sourceId: string;
  title: string;
  excerpt: string;
  citation: string;
  pageNumber: number | null;
  sourceSection: string | null;
  retrievalPath: string;
  citationQuality: "HIGH" | "MEDIUM" | "LOW";
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
  retrievalMode: string;
  warnings: string[];
  createdAt: string;
}

export function searchClientKnowledge(clientId: string, query: string) {
  if (isDemoDataEnabled) {
    return searchDemoClientKnowledge(clientId, query);
  }
  const search = query ? `?query=${encodeURIComponent(query)}` : "";
  return apiClient.get<SearchResult[]>(`/api/clients/${clientId}/search${search}`);
}

export function askClientQuestion(clientId: string, question: string) {
  if (isDemoDataEnabled) {
    return askDemoClientQuestion(clientId, question);
  }
  return apiClient.post<AskClientResponse>(`/api/clients/${clientId}/ask`, { question });
}

export function sendAiFeedback(interactionId: string, helpful: boolean, comment?: string) {
  if (isDemoDataEnabled) {
    return sendDemoAiFeedback(interactionId, helpful);
  }
  return apiClient.post<void>(`/api/ai-interactions/${interactionId}/feedback`, { helpful, comment });
}
