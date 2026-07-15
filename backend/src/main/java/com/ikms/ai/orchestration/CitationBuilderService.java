package com.ikms.ai.orchestration;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CitationBuilderService {

  public List<EnterpriseAiContracts.CitationReference> buildCitations(
      List<EnterpriseAiContracts.RetrievedEvidence> evidence) {
    return evidence.stream()
        .map(item -> new EnterpriseAiContracts.CitationReference(
            item.sourceType(),
            item.sourceId(),
            item.title(),
            item.excerpt(),
            item.pageNumber(),
            null,
            item.sourceSection(),
            normalizeConfidence(item.citationQuality(), item.pageNumber(), item.sourceSection()),
            buildJumpTargetId(item),
            item.retrievalPath()))
        .toList();
  }

  public List<EnterpriseAiContracts.EvidenceReference> buildEvidenceReferences(
      List<EnterpriseAiContracts.CitationReference> citations) {
    return citations.stream()
        .map(citation -> new EnterpriseAiContracts.EvidenceReference(
            citation.jumpTargetId(),
            citation.pageNumber() != null ? "Jump to page" : citation.section() != null ? "Jump to metadata" : "Jump to source",
            citation.pageNumber() != null ? "page" : citation.section() != null ? "metadata" : "source",
            citation.pageNumber() != null ? "Page " + citation.pageNumber() : citation.section() == null ? citation.title() : citation.section(),
            true))
        .toList();
  }

  public List<EnterpriseAiContracts.SourceReference> buildSourceReferences(
      List<EnterpriseAiContracts.CitationReference> citations) {
    return citations.stream()
        .map(citation -> new EnterpriseAiContracts.SourceReference(
            citation.sourceType() + ":" + citation.sourceId(),
            citation.title(),
            normalizeSourceKind(citation.sourceType())))
        .distinct()
        .toList();
  }

  private static String buildJumpTargetId(EnterpriseAiContracts.RetrievedEvidence item) {
    if (item.pageNumber() != null) {
      return "document:" + item.sourceId() + ":page:" + item.pageNumber();
    }
    if (item.sourceSection() != null && !item.sourceSection().isBlank()) {
      return "metadata:" + item.sourceType() + ":" + item.sourceId() + ":" + item.sourceSection();
    }
    return item.sourceType().toLowerCase() + ":" + item.sourceId();
  }

  private static String normalizeSourceKind(String sourceType) {
    return switch (sourceType.toUpperCase()) {
      case "DOCUMENT" -> "DOCUMENT";
      case "DOCUMENT_VERSION" -> "DOCUMENT";
      case "EMAIL" -> "EMAIL";
      case "NOTE" -> "NOTE";
      case "CUSTOMER" -> "CUSTOMER";
      case "OCR_TEXT", "EXTRACTED_FIELD" -> "METADATA";
      default -> "UNKNOWN";
    };
  }

  private static String normalizeConfidence(String citationQuality, Integer pageNumber, String section) {
    if (citationQuality != null && !citationQuality.isBlank()) {
      return citationQuality.toUpperCase();
    }
    if (pageNumber != null) {
      return "HIGH";
    }
    if (section != null && !section.isBlank()) {
      return "MEDIUM";
    }
    return "UNKNOWN";
  }
}
