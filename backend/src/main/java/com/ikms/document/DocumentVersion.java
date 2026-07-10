package com.ikms.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_version")
public class DocumentVersion {

  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "document_id", nullable = false)
  private Document document;

  @Column(name = "version_number", nullable = false)
  private int versionNumber;

  @Column(name = "file_hash", nullable = false, unique = true, length = 128)
  private String fileHash;

  @Column(name = "file_name", nullable = false, length = 255)
  private String fileName;

  @Column(name = "mime_type", nullable = false, length = 160)
  private String mimeType;

  @Column(name = "file_size_bytes", nullable = false)
  private long fileSizeBytes;

  @Column(name = "original_storage_path", nullable = false, length = 512)
  private String originalStoragePath;

  @Column(name = "redacted_storage_path", length = 512)
  private String redactedStoragePath;

  @Column(name = "extracted_text", columnDefinition = "TEXT")
  private String extractedText;

  @Column(name = "ocr_provider", length = 120)
  private String ocrProvider;

  @Column(name = "embedding_model", length = 120)
  private String embeddingModel;

  @Column(length = 32)
  private String language;

  @Enumerated(EnumType.STRING)
  @Column(name = "redaction_status", nullable = false, length = 32)
  private RedactionStatus redactionStatus = RedactionStatus.PENDING;

  @Column(name = "is_current", nullable = false)
  private boolean current = true;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "created_by")
  private UUID createdBy;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    createdAt = createdAt == null ? Instant.now() : createdAt;
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public Document getDocument() { return document; }
  public void setDocument(Document document) { this.document = document; }
  public int getVersionNumber() { return versionNumber; }
  public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }
  public String getFileHash() { return fileHash; }
  public void setFileHash(String fileHash) { this.fileHash = fileHash; }
  public String getFileName() { return fileName; }
  public void setFileName(String fileName) { this.fileName = fileName; }
  public String getMimeType() { return mimeType; }
  public void setMimeType(String mimeType) { this.mimeType = mimeType; }
  public long getFileSizeBytes() { return fileSizeBytes; }
  public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
  public String getOriginalStoragePath() { return originalStoragePath; }
  public void setOriginalStoragePath(String originalStoragePath) { this.originalStoragePath = originalStoragePath; }
  public String getRedactedStoragePath() { return redactedStoragePath; }
  public void setRedactedStoragePath(String redactedStoragePath) { this.redactedStoragePath = redactedStoragePath; }
  public String getExtractedText() { return extractedText; }
  public void setExtractedText(String extractedText) { this.extractedText = extractedText; }
  public String getOcrProvider() { return ocrProvider; }
  public void setOcrProvider(String ocrProvider) { this.ocrProvider = ocrProvider; }
  public String getEmbeddingModel() { return embeddingModel; }
  public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
  public String getLanguage() { return language; }
  public void setLanguage(String language) { this.language = language; }
  public RedactionStatus getRedactionStatus() { return redactionStatus; }
  public void setRedactionStatus(RedactionStatus redactionStatus) { this.redactionStatus = redactionStatus; }
  public boolean isCurrent() { return current; }
  public void setCurrent(boolean current) { this.current = current; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public UUID getCreatedBy() { return createdBy; }
  public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
}
