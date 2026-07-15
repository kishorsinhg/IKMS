package com.ikms.document;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ikms.processing.validation")
public class DocumentProcessingValidationProperties {

  private BigDecimal lowConfidenceThreshold = new BigDecimal("0.7500");
  private BigDecimal unreadableOcrThreshold = new BigDecimal("0.4500");
  private List<String> requiredFields = new ArrayList<>(List.of("documentType", "insurer"));
  private List<String> requiredBusinessReferenceFields = new ArrayList<>(List.of("policyNumber", "claimNumber", "brokerReference"));

  public BigDecimal getLowConfidenceThreshold() { return lowConfidenceThreshold; }
  public void setLowConfidenceThreshold(BigDecimal lowConfidenceThreshold) { this.lowConfidenceThreshold = lowConfidenceThreshold; }
  public BigDecimal getUnreadableOcrThreshold() { return unreadableOcrThreshold; }
  public void setUnreadableOcrThreshold(BigDecimal unreadableOcrThreshold) { this.unreadableOcrThreshold = unreadableOcrThreshold; }
  public List<String> getRequiredFields() { return requiredFields; }
  public void setRequiredFields(List<String> requiredFields) { this.requiredFields = requiredFields; }
  public List<String> getRequiredBusinessReferenceFields() { return requiredBusinessReferenceFields; }
  public void setRequiredBusinessReferenceFields(List<String> requiredBusinessReferenceFields) { this.requiredBusinessReferenceFields = requiredBusinessReferenceFields; }
}
