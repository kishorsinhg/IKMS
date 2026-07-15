package com.ikms.document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DocumentValidationService {

  private final DocumentProcessingValidationProperties properties;

  public DocumentValidationService(DocumentProcessingValidationProperties properties) {
    this.properties = properties;
  }

  public ValidationResult validate(
      UUID clientIdHint,
      List<DocumentMetadataExtractionService.ExtractedField> fields,
      BigDecimal ocrConfidence,
      BigDecimal classificationConfidence,
      BigDecimal duplicateConfidence) {
    List<FindingDraft> findings = new ArrayList<>();

    if (clientIdHint == null) {
      findings.add(new FindingDraft(
          "missing-customer",
          DocumentProcessingFindingSeverity.ERROR,
          DocumentProcessingStage.VALIDATION,
          "customerId",
          "Customer association is missing and requires reviewer confirmation.",
          null,
          null,
          classificationConfidence));
    }

    for (DocumentMetadataExtractionService.ExtractedField field : fields) {
      boolean required = isRequired(field);
      if (required && field.empty()) {
        findings.add(new FindingDraft(
            "missing-field-" + field.fieldKey(),
            DocumentProcessingFindingSeverity.ERROR,
            DocumentProcessingStage.VALIDATION,
            field.fieldKey(),
            field.fieldLabel() + " is required before approval.",
            null,
            field.sourcePage(),
            field.confidence()));
      } else if (field.confidence() != null && field.confidence().compareTo(properties.getLowConfidenceThreshold()) < 0) {
        findings.add(new FindingDraft(
            "low-confidence-" + field.fieldKey(),
            DocumentProcessingFindingSeverity.WARNING,
            DocumentProcessingStage.CONFIDENCE_CALCULATION,
            field.fieldKey(),
            field.fieldLabel() + " was extracted with low confidence and should be reviewed.",
            field.normalizedValue(),
            field.sourcePage(),
            field.confidence()));
      }
    }

    if (ocrConfidence != null && ocrConfidence.compareTo(properties.getUnreadableOcrThreshold()) < 0) {
      findings.add(new FindingDraft(
          "unreadable-ocr",
          DocumentProcessingFindingSeverity.ERROR,
          DocumentProcessingStage.OCR_TEXT_EXTRACTION,
          null,
          "OCR quality is too low for straight-through publication.",
          null,
          null,
          ocrConfidence));
    }

    if (duplicateConfidence != null && duplicateConfidence.compareTo(new BigDecimal("0.9000")) >= 0) {
      findings.add(new FindingDraft(
          "possible-duplicate",
          DocumentProcessingFindingSeverity.WARNING,
          DocumentProcessingStage.VALIDATION,
          null,
          "This document appears related to previously processed knowledge and requires reviewer confirmation.",
          null,
          null,
          duplicateConfidence));
    }

    BigDecimal validationConfidence = score(findings);
    boolean requiresReview = findings.stream().anyMatch(finding -> finding.severity() == DocumentProcessingFindingSeverity.ERROR)
        || findings.stream().anyMatch(finding -> finding.severity() == DocumentProcessingFindingSeverity.WARNING);
    return new ValidationResult(findings, validationConfidence, requiresReview);
  }

  private boolean isRequired(DocumentMetadataExtractionService.ExtractedField field) {
    String key = field.normalizedFieldKey();
    return properties.getRequiredFields().stream().anyMatch(required -> required.equalsIgnoreCase(key))
        || properties.getRequiredBusinessReferenceFields().stream().anyMatch(required -> required.equalsIgnoreCase(key))
        || field.required();
  }

  private BigDecimal score(List<FindingDraft> findings) {
    if (findings.isEmpty()) {
      return new BigDecimal("0.9800");
    }
    int penalty = findings.stream()
        .mapToInt(finding -> finding.severity() == DocumentProcessingFindingSeverity.ERROR ? 30 : 10)
        .sum();
    int raw = Math.max(10, 100 - penalty);
    return BigDecimal.valueOf(raw).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
  }

  public record ValidationResult(
      List<FindingDraft> findings,
      BigDecimal validationConfidence,
      boolean requiresReview) {
  }

  public record FindingDraft(
      String code,
      DocumentProcessingFindingSeverity severity,
      DocumentProcessingStage stage,
      String fieldKey,
      String message,
      String evidenceText,
      Integer sourcePage,
      BigDecimal confidence) {
    public String normalizedFieldKey() {
      return fieldKey == null ? null : fieldKey.toLowerCase(Locale.ROOT);
    }
  }
}
