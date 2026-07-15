import { ReactNode } from "react";

export type ConfidenceLevel = "HIGH" | "MEDIUM" | "LOW" | "UNKNOWN";
export type MetadataFieldState = "VERIFIED" | "NEEDS_REVIEW" | "MISSING" | "READ_ONLY";
export type AiSummaryState = "loading" | "ready" | "unavailable" | "error";
export type EvidenceNavigationKind = "page" | "ocr-region" | "highlight";

export interface EvidenceNavigationAction {
  key: string;
  label: string;
  kind: EvidenceNavigationKind;
  disabled?: boolean;
  onClick?: () => void;
}

export interface EvidenceWorkspaceSection {
  key: string;
  title: string;
  content: ReactNode;
  defaultExpanded?: boolean;
  summary?: string;
  searchText?: string;
  countLabel?: string;
}

export interface MetadataFieldDescriptor {
  key: string;
  label: string;
  value?: string | null;
  state: MetadataFieldState;
  helperText?: string;
  confidence?: ConfidenceLevel;
}

export function normalizeConfidence(value: string | null | undefined): ConfidenceLevel {
  const normalized = value?.trim().toUpperCase();
  if (normalized === "HIGH") {
    return "HIGH";
  }
  if (normalized === "MEDIUM") {
    return "MEDIUM";
  }
  if (normalized === "LOW") {
    return "LOW";
  }
  return "UNKNOWN";
}
