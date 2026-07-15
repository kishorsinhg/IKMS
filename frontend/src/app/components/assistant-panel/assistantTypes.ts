export type AssistantSurfaceVariant = "embedded" | "drawer" | "sheet";

export type AssistantConversationState = "empty" | "loading" | "ready" | "error";

export type AssistantMessageRole = "user" | "assistant";

export type AssistantSourceKind = "DOCUMENT" | "EMAIL" | "METADATA" | "CUSTOMER" | "NOTE" | "UNKNOWN";

export type AssistantEvidenceTarget = "page" | "ocr-region" | "metadata" | "customer";
export type AssistantReferenceType = "policy" | "claim";

export interface AssistantSourceReference {
  key: string;
  label: string;
  kind: AssistantSourceKind;
  referenceType?: AssistantReferenceType;
  detail?: string;
  onClick?: () => void;
  disabled?: boolean;
}

export interface AssistantEvidenceReference {
  key: string;
  label: string;
  target: AssistantEvidenceTarget;
  detail?: string;
  onClick?: () => void;
  disabled?: boolean;
}

export interface AssistantMessage {
  id: string;
  role: AssistantMessageRole;
  content?: string;
  timestamp?: string;
  status?: "ready" | "streaming" | "error";
  warnings?: string[];
  sourceReferences?: AssistantSourceReference[];
  evidenceReferences?: AssistantEvidenceReference[];
}

export interface SuggestedQuestion {
  key: string;
  label: string;
  onSelect?: () => void;
  disabled?: boolean;
}
