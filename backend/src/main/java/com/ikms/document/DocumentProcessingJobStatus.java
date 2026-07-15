package com.ikms.document;

public enum DocumentProcessingJobStatus {
  QUEUED,
  RUNNING,
  WAITING_REVIEW,
  APPROVED,
  REJECTED,
  FAILED,
  RETRYING,
  COMPLETED
}
