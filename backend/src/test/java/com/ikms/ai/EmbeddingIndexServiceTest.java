package com.ikms.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.ikms.config.domain.MetadataField;
import com.ikms.config.domain.MetadataValue;
import com.ikms.worker.extract.TextExtractionService.PageSegment;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EmbeddingIndexServiceTest {

  @Test
  void semanticChunksShouldPreserveSentenceContextAndMetadata() {
    String text = """
        Policy renewal is due on 01 August 2026. Carrier requested updated payroll estimates.

        The insured confirmed headcount growth in the manufacturing unit. A supervisor note records the expected premium impact.
        """;

    var chunks = EmbeddingIndexService.semanticChunks(
        java.util.List.of(new PageSegment(2, text)),
        "Policy Renewal",
        "en",
        "document-version");

    assertThat(chunks).isNotEmpty();
    assertThat(chunks.get(0).sourceTitle()).isEqualTo("Policy Renewal");
    assertThat(chunks.get(0).language()).isEqualTo("en");
    assertThat(chunks.get(0).pageNumber()).isEqualTo(2);
    assertThat(chunks.get(0).metadataSummary()).contains("title=Policy Renewal");
    assertThat(chunks.stream().map(EmbeddingIndexService.ChunkDescriptor::text).collect(java.util.stream.Collectors.joining(" ")))
        .contains("Policy renewal is due on 01 August 2026")
        .contains("headcount growth");
    assertThat(chunks).allMatch(chunk -> chunk.tokenCount() > 0);
  }

  @Test
  void businessReferenceProjectionShouldTreatInsuranceValuesAsStructuredIndexFields() {
    MetadataField policyField = new MetadataField();
    policyField.setId(UUID.randomUUID());
    policyField.setFieldKey("policy_number");
    policyField.setLabel("Policy Number");

    MetadataField claimField = new MetadataField();
    claimField.setId(UUID.randomUUID());
    claimField.setFieldKey("claim_number");
    claimField.setLabel("Claim Number");

    MetadataField insurerField = new MetadataField();
    insurerField.setId(UUID.randomUUID());
    insurerField.setFieldKey("insurer");
    insurerField.setLabel("Insurer");

    MetadataField effectiveField = new MetadataField();
    effectiveField.setId(UUID.randomUUID());
    effectiveField.setFieldKey("effective_date");
    effectiveField.setLabel("Effective Date");

    MetadataValue policyValue = new MetadataValue();
    policyValue.setField(policyField);
    policyValue.setTextValue("POL-12345");

    MetadataValue claimValue = new MetadataValue();
    claimValue.setField(claimField);
    claimValue.setTextValue("CLM-9988");

    MetadataValue insurerValue = new MetadataValue();
    insurerValue.setField(insurerField);
    insurerValue.setTextValue("Northwind Mutual");

    MetadataValue effectiveValue = new MetadataValue();
    effectiveValue.setField(effectiveField);
    effectiveValue.setTextValue("2026-08-01");

    EmbeddingIndexService.BusinessReferenceProjection projection =
        EmbeddingIndexService.businessReferencesFromMetadata(
            java.util.List.of(policyValue, claimValue, insurerValue, effectiveValue));

    assertThat(projection.policyNumber()).isEqualTo("POL-12345");
    assertThat(projection.claimNumber()).isEqualTo("CLM-9988");
    assertThat(projection.insurer()).isEqualTo("Northwind Mutual");
    assertThat(projection.effectiveDate()).isEqualTo(LocalDate.parse("2026-08-01"));

    var chunks = EmbeddingIndexService.semanticChunks(
        java.util.List.of(new PageSegment(1, "Renewal notice for policy POL-12345")),
        "Renewal Notice",
        "en",
        "document-version",
        projection);

    assertThat(chunks.getFirst().metadataSummary())
        .contains("policy_number=POL-12345")
        .contains("claim_number=CLM-9988")
        .contains("insurer=Northwind Mutual");
  }
}
