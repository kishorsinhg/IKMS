package com.ikms.config.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_provider_setting")
public class AiProviderSetting {

  @Id
  private UUID id;

  @Column(name = "provider_name", nullable = false, length = 80)
  private String providerName;

  @Column(name = "model_name", nullable = false, length = 120)
  private String modelName;

  @Column(name = "embedding_model_name", nullable = false, length = 120)
  private String embeddingModelName;

  @Column(name = "api_base_url", length = 512)
  private String apiBaseUrl;

  @Column(name = "api_key", length = 512)
  private String apiKey;

  @Column(name = "ocr_provider", nullable = false, length = 80)
  private String ocrProvider;

  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    updatedAt = updatedAt == null ? Instant.now() : updatedAt;
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getProviderName() { return providerName; }
  public void setProviderName(String providerName) { this.providerName = providerName; }
  public String getModelName() { return modelName; }
  public void setModelName(String modelName) { this.modelName = modelName; }
  public String getEmbeddingModelName() { return embeddingModelName; }
  public void setEmbeddingModelName(String embeddingModelName) { this.embeddingModelName = embeddingModelName; }
  public String getApiBaseUrl() { return apiBaseUrl; }
  public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }
  public String getApiKey() { return apiKey; }
  public void setApiKey(String apiKey) { this.apiKey = apiKey; }
  public String getOcrProvider() { return ocrProvider; }
  public void setOcrProvider(String ocrProvider) { this.ocrProvider = ocrProvider; }
  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
