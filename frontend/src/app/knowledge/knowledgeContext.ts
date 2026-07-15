import type {
  CustomerKnowledgeTimelineEvent,
  DocumentVersionSummary,
  RelatedKnowledgeLink,
} from "../../api/knowledge";
import type {
  AssistantEvidenceReference,
  AssistantEvidenceTarget,
  AssistantSourceReference,
} from "../components/assistant-panel/assistantTypes";

export function humanizeKnowledgeRelationshipType(value: string) {
  return value
    .toLowerCase()
    .replace(/_/g, " ")
    .replace(/\b\w/g, (character) => character.toUpperCase());
}

export function humanizeKnowledgeTimelineEventType(value: string) {
  return value
    .toLowerCase()
    .replace(/_/g, " ")
    .replace(/\b\w/g, (character) => character.toUpperCase());
}

export function normalizeKnowledgeSourceKind(value: string): AssistantSourceReference["kind"] {
  switch (value.toUpperCase()) {
    case "DOCUMENT":
    case "DOCUMENT_VERSION":
      return "DOCUMENT";
    case "EMAIL":
      return "EMAIL";
    case "NOTE":
      return "NOTE";
    case "CUSTOMER":
      return "CUSTOMER";
    default:
      return "METADATA";
  }
}

export function collectKnowledgeBusinessReferenceFields(links: RelatedKnowledgeLink[]) {
  const seen = new Set<string>();
  const references: Array<{
    key: string;
    label: string;
    value: string;
    referenceType?: AssistantSourceReference["referenceType"];
  }> = [];

  for (const link of links) {
    Object.entries(link.supportingFields ?? {}).forEach(([key, value]) => {
      if (!value || seen.has(`${key}:${value}`)) {
        return;
      }
      if (!["policy_number", "claim_number", "insurer", "broker_reference", "external_reference"].includes(key)) {
        return;
      }
      seen.add(`${key}:${value}`);
      references.push({
        key,
        label: key
          .replace(/_/g, " ")
          .replace(/\b\w/g, (character) => character.toUpperCase()),
        value,
        referenceType: key === "policy_number" ? "policy" : key === "claim_number" ? "claim" : undefined,
      });
    });
  }

  return references;
}

export function buildKnowledgeAssistantEvidenceReferences(
  links: RelatedKnowledgeLink[],
  timelineEvents: CustomerKnowledgeTimelineEvent[] = [],
  fallback?: AssistantEvidenceReference,
): AssistantEvidenceReference[] {
  const references = links.slice(0, 4).map<AssistantEvidenceReference>((link) => {
    const target: AssistantEvidenceTarget = link.relatedSourceType === "DOCUMENT" || link.relatedSourceType === "DOCUMENT_VERSION"
      ? "page"
      : "metadata";
    return {
      key: `related-${link.relationshipId}`,
      label: link.relatedTitle,
      target,
      detail: humanizeKnowledgeRelationshipType(link.relationshipType),
      disabled: true,
    };
  });

  if (references.length === 0) {
    references.push(
      ...timelineEvents.slice(0, 2).map((event) => {
        const target: AssistantEvidenceTarget = event.sourceType === "DOCUMENT" || event.sourceType === "DOCUMENT_VERSION"
          ? "page"
          : "metadata";
        return {
          key: `timeline-${event.eventId}`,
          label: event.title,
          target,
          detail: humanizeKnowledgeTimelineEventType(event.eventType),
          disabled: true,
        };
      }),
    );
  }

  if (references.length === 0 && fallback) {
    references.push(fallback);
  }

  return references;
}

export function buildKnowledgeAssistantSourceReferences({
  customerLabel,
  primarySource,
  relatedLinks,
}: {
  customerLabel?: string | null;
  primarySource?: {
    key: string;
    label: string;
    sourceType: string;
    detail?: string;
  } | null;
  relatedLinks: RelatedKnowledgeLink[];
}): AssistantSourceReference[] {
  const sources: AssistantSourceReference[] = [];

  if (primarySource) {
    sources.push({
      key: primarySource.key,
      label: primarySource.label,
      kind: normalizeKnowledgeSourceKind(primarySource.sourceType),
      detail: primarySource.detail,
      disabled: true,
    });
  }

  if (customerLabel) {
    sources.push({
      key: "customer-context",
      label: customerLabel,
      kind: "CUSTOMER",
      disabled: true,
    });
  }

  relatedLinks.slice(0, 3).forEach((link) => {
    sources.push({
      key: `source-${link.relationshipId}`,
      label: link.relatedTitle,
      kind: normalizeKnowledgeSourceKind(link.relatedSourceType),
      detail: humanizeKnowledgeRelationshipType(link.relationshipType),
      disabled: true,
    });
  });

  collectKnowledgeBusinessReferenceFields(relatedLinks).slice(0, 3).forEach((reference, index) => {
    sources.push({
      key: `business-reference-${index}`,
      label: reference.value,
      kind: "METADATA",
      referenceType: reference.referenceType,
      detail: reference.label,
      disabled: true,
    });
  });

  return dedupeAssistantSources(sources);
}

export function summarizeKnowledgeVersions(versions: DocumentVersionSummary[]) {
  if (versions.length === 0) {
    return "Version history is not available from the current API.";
  }

  return versions
    .map((version) => `v${version.versionNumber}${version.current ? " (current)" : ""}`)
    .join(" | ");
}

function dedupeAssistantSources(sources: AssistantSourceReference[]) {
  const seen = new Set<string>();
  return sources.filter((source) => {
    const key = `${source.kind}:${source.label}:${source.detail ?? ""}`;
    if (seen.has(key)) {
      return false;
    }
    seen.add(key);
    return true;
  });
}
