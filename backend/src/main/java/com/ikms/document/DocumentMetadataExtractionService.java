package com.ikms.document;

import com.ikms.ai.ClassificationService;
import com.ikms.ai.orchestration.BusinessReferenceExtractionService;
import com.ikms.ai.orchestration.EnterpriseAiContracts;
import com.ikms.worker.extract.TextExtractionService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class DocumentMetadataExtractionService {

  private static final Pattern ISO_DATE_PATTERN = Pattern.compile("\\b(20\\d{2}-\\d{2}-\\d{2})\\b");

  private final BusinessReferenceExtractionService businessReferenceExtractionService;

  public DocumentMetadataExtractionService(BusinessReferenceExtractionService businessReferenceExtractionService) {
    this.businessReferenceExtractionService = businessReferenceExtractionService;
  }

  public ExtractionBundle extract(
      Document document,
      DocumentVersion version,
      TextExtractionService.ExtractionResult extraction,
      ClassificationService.ClassificationResult classification,
      String detectedLanguage) {
    List<ExtractedField> fields = new ArrayList<>();
    String extractedText = extraction.extractedText() == null ? "" : extraction.extractedText();
    Map<String, Object> hints = Map.of(
        "policyNumber", safeMetadata(classification.metadata(), "policyNumber"),
        "claimNumber", safeMetadata(classification.metadata(), "claimNumber"),
        "insurer", safeMetadata(classification.metadata(), "insurer"),
        "brokerReference", safeMetadata(classification.metadata(), "brokerReference"),
        "effectiveDate", safeMetadata(classification.metadata(), "effectiveDate"),
        "expiryDate", safeMetadata(classification.metadata(), "expiryDate"),
        "renewalDate", safeMetadata(classification.metadata(), "renewalDate"));
    EnterpriseAiContracts.BusinessReferenceFields references = businessReferenceExtractionService.extract(extractedText, hints);

    fields.add(field("documentType", "Document Type", DocumentProcessingFieldType.DOCUMENT_METADATA,
        safeMetadata(classification.metadata(), "documentType"), classification.classificationConfidence(), "CLASSIFICATION", "provider-classification", null, true, null));
    fields.add(field("language", "Language", DocumentProcessingFieldType.DOCUMENT_METADATA,
        detectedLanguage, confidenceOrDefault(classification.classificationConfidence(), "0.9000"), "OCR_TEXT", "language-detection", null, true, null));
    fields.add(field("documentTitle", "Document Title", DocumentProcessingFieldType.DOCUMENT_METADATA,
        classification.suggestedTitle(), confidenceOrDefault(classification.classificationConfidence(), "0.8800"), "FILENAME", "title-normalization", null, true, null));
    fields.add(field("documentDate", "Document Date", DocumentProcessingFieldType.DOCUMENT_METADATA,
        firstDate(extractedText), confidenceOrDefault(extraction.extractionConfidence(), "0.7200"), "OCR_TEXT", "regex-date", firstPage(extraction.segments()), false, null));
    fields.add(field("customerId", "Customer Identifier", DocumentProcessingFieldType.CUSTOMER_IDENTIFIER,
        document.getClient() == null ? null : document.getClient().getClientId(),
        classification.clientMatchConfidence(), "CLASSIFICATION", "client-match", null, false, null));

    addBusinessReferenceField(fields, "policyNumber", "Policy Number", references.policyNumber(), extraction, "policy");
    addBusinessReferenceField(fields, "claimNumber", "Claim Number", references.claimNumber(), extraction, "claim");
    addBusinessReferenceField(fields, "brokerReference", "Broker Reference", references.brokerReference(), extraction, "brokerReference");
    addBusinessReferenceField(fields, "insurer", "Insurer", references.insurer(), extraction, "insurer");
    addBusinessReferenceField(fields, "effectiveDate", "Effective Date", references.effectiveDate(), extraction, "effectiveDate");
    addBusinessReferenceField(fields, "expiryDate", "Expiry Date", references.expiryDate(), extraction, "expiryDate");
    addBusinessReferenceField(fields, "renewalDate", "Renewal Date", references.renewalDate(), extraction, "renewalDate");

    BigDecimal metadataConfidence = average(fields.stream()
        .filter(field -> field.fieldType() != DocumentProcessingFieldType.BUSINESS_REFERENCE)
        .map(ExtractedField::confidence)
        .toList());
    BigDecimal businessReferenceConfidence = average(fields.stream()
        .filter(field -> field.fieldType() == DocumentProcessingFieldType.BUSINESS_REFERENCE)
        .map(ExtractedField::confidence)
        .toList());

    return new ExtractionBundle(fields, metadataConfidence, businessReferenceConfidence);
  }

  private void addBusinessReferenceField(
      List<ExtractedField> fields,
      String key,
      String label,
      String value,
      TextExtractionService.ExtractionResult extraction,
      String businessReferenceType) {
    fields.add(field(
        key,
        label,
        DocumentProcessingFieldType.BUSINESS_REFERENCE,
        value,
        confidenceOrDefault(extraction.extractionConfidence(), "0.7800"),
        "OCR_TEXT",
        "business-reference-extraction",
        firstPage(extraction.segments()),
        false,
        businessReferenceType));
  }

  private static ExtractedField field(
      String key,
      String label,
      DocumentProcessingFieldType fieldType,
      String value,
      BigDecimal confidence,
      String sourceType,
      String extractionMethod,
      Integer sourcePage,
      boolean required,
      String businessReferenceType) {
    return new ExtractedField(key, label, fieldType, value, confidence, sourceType, extractionMethod, sourcePage, required, businessReferenceType);
  }

  private static String safeMetadata(Map<String, String> metadata, String key) {
    return metadata == null ? null : metadata.get(key);
  }

  private static Integer firstPage(List<TextExtractionService.PageSegment> segments) {
    if (segments == null || segments.isEmpty()) {
      return null;
    }
    return segments.get(0).pageNumber();
  }

  private static String firstDate(String extractedText) {
    Matcher matcher = ISO_DATE_PATTERN.matcher(extractedText == null ? "" : extractedText);
    return matcher.find() ? matcher.group(1) : null;
  }

  private static BigDecimal confidenceOrDefault(BigDecimal value, String fallback) {
    return value == null ? new BigDecimal(fallback) : value;
  }

  private static BigDecimal average(List<BigDecimal> confidences) {
    List<BigDecimal> nonNull = confidences.stream().filter(value -> value != null).toList();
    if (nonNull.isEmpty()) {
      return null;
    }
    BigDecimal total = nonNull.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    return total.divide(BigDecimal.valueOf(nonNull.size()), 4, java.math.RoundingMode.HALF_UP);
  }

  public record ExtractionBundle(
      List<ExtractedField> fields,
      BigDecimal metadataConfidence,
      BigDecimal businessReferenceConfidence) {
  }

  public record ExtractedField(
      String fieldKey,
      String fieldLabel,
      DocumentProcessingFieldType fieldType,
      String value,
      BigDecimal confidence,
      String sourceType,
      String extractionMethod,
      Integer sourcePage,
      boolean required,
      String businessReferenceType) {
    public String normalizedValue() {
      return value == null ? null : value.trim();
    }

    public boolean empty() {
      return normalizedValue() == null || normalizedValue().isEmpty();
    }

    public String normalizedFieldKey() {
      return fieldKey == null ? "" : fieldKey.trim().toLowerCase(Locale.ROOT);
    }
  }
}
