package com.ikms.common.api;

import java.time.Instant;
import java.util.List;

public record ApiError(
    Instant timestamp,
    int status,
    String error,
    String code,
    String message,
    String path,
    String requestId,
    String correlationId,
    List<FieldViolation> violations) {

  public ApiError {
    violations = violations == null ? List.of() : List.copyOf(violations);
  }

  public record FieldViolation(String field, String message) {
  }
}
