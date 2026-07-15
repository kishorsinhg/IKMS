package com.ikms.document;

public enum DocumentProcessingStatus {
  INTAKE_RECEIVED,
  VIRUS_SCANNED,
  EXTRACTING,
  OCR_COMPLETE,
  CLASSIFIED,
  VALIDATED,
  WAITING_REVIEW,
  APPROVED,
  PUBLISHED,
  INDEXED,
  FAILED
}
