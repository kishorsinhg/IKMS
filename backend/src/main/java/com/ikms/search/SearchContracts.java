package com.ikms.search;

import java.time.Instant;
import java.util.UUID;

public final class SearchContracts {

  private SearchContracts() {
  }

  public record SearchResultResponse(
      String sourceType,
      UUID sourceId,
      String title,
      String excerpt,
      String citation,
      Integer pageNumber,
      String sourceSection,
      String retrievalPath,
      String citationQuality,
      Instant occurredAt) {
  }
}
