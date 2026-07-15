package com.ikms.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "embedding_chunk")
public class EmbeddingChunk {

  @Id
  private UUID id;

  @Column(name = "client_id", nullable = false)
  private UUID clientId;

  @Column(name = "source_type", nullable = false, length = 32)
  private String sourceType;

  @Column(name = "source_id", nullable = false)
  private UUID sourceId;

  @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
  private String chunkText;

  @Column(name = "chunk_index", nullable = false)
  private int chunkIndex;

  @Column(name = "token_count")
  private Integer tokenCount;

  @Column(name = "source_title", length = 255)
  private String sourceTitle;

  @Column(name = "source_section", length = 255)
  private String sourceSection;

  @Column(name = "document_id")
  private UUID documentId;

  @Column(name = "document_version_id")
  private UUID documentVersionId;

  @Column(name = "document_type_id")
  private UUID documentTypeId;

  @Column(name = "page_number")
  private Integer pageNumber;

  @Column(name = "metadata_summary", columnDefinition = "TEXT")
  private String metadataSummary;

  @Column(name = "policy_number", length = 120)
  private String policyNumber;

  @Column(name = "claim_number", length = 120)
  private String claimNumber;

  @Column(name = "insurer", length = 255)
  private String insurer;

  @Column(name = "policy_type", length = 120)
  private String policyType;

  @Column(name = "effective_date")
  private LocalDate effectiveDate;

  @Column(name = "expiry_date")
  private LocalDate expiryDate;

  @Column(name = "renewal_date")
  private LocalDate renewalDate;

  @Column(name = "broker_reference", length = 120)
  private String brokerReference;

  @Column(name = "external_reference", length = 120)
  private String externalReference;

  @Column(name = "source_system", length = 80)
  private String sourceSystem;

  @Column(name = "security_classification", length = 64)
  private String securityClassification;

  @Column(name = "acl_summary", columnDefinition = "TEXT")
  private String aclSummary;

  @Column(name = "content_hash", length = 128)
  private String contentHash;

  @Column(name = "reindex_version", nullable = false)
  private Integer reindexVersion = 1;

  @Column(length = 32)
  private String language;

  @Column(name = "embedding_reference", length = 255)
  private String embeddingReference;

  @Column(name = "embedding_vector", columnDefinition = "TEXT")
  private String embeddingVector;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "indexed_at", nullable = false)
  private Instant indexedAt;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    createdAt = createdAt == null ? Instant.now() : createdAt;
    indexedAt = indexedAt == null ? createdAt : indexedAt;
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UUID getClientId() { return clientId; }
  public void setClientId(UUID clientId) { this.clientId = clientId; }
  public String getSourceType() { return sourceType; }
  public void setSourceType(String sourceType) { this.sourceType = sourceType; }
  public UUID getSourceId() { return sourceId; }
  public void setSourceId(UUID sourceId) { this.sourceId = sourceId; }
  public String getChunkText() { return chunkText; }
  public void setChunkText(String chunkText) { this.chunkText = chunkText; }
  public int getChunkIndex() { return chunkIndex; }
  public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
  public Integer getTokenCount() { return tokenCount; }
  public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }
  public String getSourceTitle() { return sourceTitle; }
  public void setSourceTitle(String sourceTitle) { this.sourceTitle = sourceTitle; }
  public String getSourceSection() { return sourceSection; }
  public void setSourceSection(String sourceSection) { this.sourceSection = sourceSection; }
  public UUID getDocumentId() { return documentId; }
  public void setDocumentId(UUID documentId) { this.documentId = documentId; }
  public UUID getDocumentVersionId() { return documentVersionId; }
  public void setDocumentVersionId(UUID documentVersionId) { this.documentVersionId = documentVersionId; }
  public UUID getDocumentTypeId() { return documentTypeId; }
  public void setDocumentTypeId(UUID documentTypeId) { this.documentTypeId = documentTypeId; }
  public Integer getPageNumber() { return pageNumber; }
  public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
  public String getMetadataSummary() { return metadataSummary; }
  public void setMetadataSummary(String metadataSummary) { this.metadataSummary = metadataSummary; }
  public String getPolicyNumber() { return policyNumber; }
  public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }
  public String getClaimNumber() { return claimNumber; }
  public void setClaimNumber(String claimNumber) { this.claimNumber = claimNumber; }
  public String getInsurer() { return insurer; }
  public void setInsurer(String insurer) { this.insurer = insurer; }
  public String getPolicyType() { return policyType; }
  public void setPolicyType(String policyType) { this.policyType = policyType; }
  public LocalDate getEffectiveDate() { return effectiveDate; }
  public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
  public LocalDate getExpiryDate() { return expiryDate; }
  public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
  public LocalDate getRenewalDate() { return renewalDate; }
  public void setRenewalDate(LocalDate renewalDate) { this.renewalDate = renewalDate; }
  public String getBrokerReference() { return brokerReference; }
  public void setBrokerReference(String brokerReference) { this.brokerReference = brokerReference; }
  public String getExternalReference() { return externalReference; }
  public void setExternalReference(String externalReference) { this.externalReference = externalReference; }
  public String getSourceSystem() { return sourceSystem; }
  public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
  public String getSecurityClassification() { return securityClassification; }
  public void setSecurityClassification(String securityClassification) { this.securityClassification = securityClassification; }
  public String getAclSummary() { return aclSummary; }
  public void setAclSummary(String aclSummary) { this.aclSummary = aclSummary; }
  public String getContentHash() { return contentHash; }
  public void setContentHash(String contentHash) { this.contentHash = contentHash; }
  public Integer getReindexVersion() { return reindexVersion; }
  public void setReindexVersion(Integer reindexVersion) { this.reindexVersion = reindexVersion; }
  public String getLanguage() { return language; }
  public void setLanguage(String language) { this.language = language; }
  public String getEmbeddingReference() { return embeddingReference; }
  public void setEmbeddingReference(String embeddingReference) { this.embeddingReference = embeddingReference; }
  public String getEmbeddingVector() { return embeddingVector; }
  public void setEmbeddingVector(String embeddingVector) { this.embeddingVector = embeddingVector; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getIndexedAt() { return indexedAt; }
  public void setIndexedAt(Instant indexedAt) { this.indexedAt = indexedAt; }
}
