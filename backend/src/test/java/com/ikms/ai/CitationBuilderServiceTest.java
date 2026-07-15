package com.ikms.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.ikms.ai.orchestration.CitationBuilderService;
import com.ikms.ai.orchestration.EnterpriseAiContracts;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CitationBuilderServiceTest {

  private final CitationBuilderService service = new CitationBuilderService();

  @Test
  void buildCitationsShouldPreferDocumentEvidenceAndMetadataKinds() {
    List<EnterpriseAiContracts.CitationReference> citations = service.buildCitations(List.of(
        new EnterpriseAiContracts.RetrievedEvidence(
            "OCR_TEXT",
            UUID.randomUUID(),
            "Policy OCR",
            "Policy Number POL-12345",
            "OCR",
            null,
            "policy-number",
            "METADATA",
            null,
            Instant.parse("2026-07-15T00:00:00Z"))));

    assertThat(citations.getFirst().confidence()).isEqualTo("MEDIUM");
    assertThat(service.buildSourceReferences(citations).getFirst().kind()).isEqualTo("METADATA");
    assertThat(service.buildEvidenceReferences(citations).getFirst().target()).isEqualTo("metadata");
  }
}
