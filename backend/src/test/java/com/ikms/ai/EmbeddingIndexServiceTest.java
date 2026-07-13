package com.ikms.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.ikms.worker.extract.TextExtractionService.PageSegment;
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
}
