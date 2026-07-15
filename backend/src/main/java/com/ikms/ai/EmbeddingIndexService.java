package com.ikms.ai;

import com.ikms.config.domain.MetadataValue;
import com.ikms.config.domain.MetadataValueRepository;
import com.ikms.document.DocumentVersion;
import com.ikms.email.Email;
import com.ikms.note.Note;
import com.ikms.worker.extract.TextExtractionService.PageSegment;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EmbeddingIndexService {

  static final int CURRENT_REINDEX_VERSION = 2;

  private final EmbeddingChunkRepository embeddingChunkRepository;
  private final AiProviderSettingsService aiProviderSettingsService;
  private final AiProviderClient aiProviderClient;
  private final MetadataValueRepository metadataValueRepository;

  public EmbeddingIndexService(
      EmbeddingChunkRepository embeddingChunkRepository,
      AiProviderSettingsService aiProviderSettingsService,
      AiProviderClient aiProviderClient,
      MetadataValueRepository metadataValueRepository) {
    this.embeddingChunkRepository = embeddingChunkRepository;
    this.aiProviderSettingsService = aiProviderSettingsService;
    this.aiProviderClient = aiProviderClient;
    this.metadataValueRepository = metadataValueRepository;
  }

  public List<EmbeddingChunk> indexDocumentVersion(UUID clientId, DocumentVersion version) {
    return indexDocumentVersion(clientId, version, List.of(new PageSegment(null, version.getExtractedText())));
  }

  public List<EmbeddingChunk> indexDocumentVersion(UUID clientId, DocumentVersion version, List<PageSegment> pageSegments) {
    version.setEmbeddingModel(aiProviderSettingsService.current().embeddingModelName());
    IndexProjection projection = projectionForDocumentVersion(clientId, version);
    return saveChunks(
        clientId,
        version.getId(),
        "DOCUMENT",
        semanticChunks(pageSegments, version.getFileName(), version.getLanguage(), "document-version", projection.businessReferences()),
        projection);
  }

  public List<EmbeddingChunk> indexEmail(UUID clientId, Email email) {
    IndexProjection projection = projectionForAuxiliarySource(clientId, "EMAIL", email.getId(), "EMAIL", email.getBodyText());
    return saveChunks(
        clientId,
        email.getId(),
        "EMAIL",
        semanticChunks(List.of(new PageSegment(null, email.getBodyText())), email.getSubject(), inferLanguage(email.getBodyText()), "email", projection.businessReferences()),
        projection);
  }

  public List<EmbeddingChunk> indexNote(UUID clientId, Note note) {
    IndexProjection projection = projectionForAuxiliarySource(clientId, "NOTE", note.getId(), "NOTE", note.getNoteText());
    return saveChunks(
        clientId,
        note.getId(),
        "NOTE",
        semanticChunks(List.of(new PageSegment(null, note.getNoteText())), "Broker note", inferLanguage(note.getNoteText()), "note", projection.businessReferences()),
        projection);
  }

  public void deleteSource(String sourceType, UUID sourceId) {
    embeddingChunkRepository.deleteBySourceTypeAndSourceId(sourceType, sourceId);
  }

  private List<EmbeddingChunk> saveChunks(
      UUID clientId,
      UUID sourceId,
      String sourceType,
      List<ChunkDescriptor> chunks,
      IndexProjection projection) {
    var providerSettings = aiProviderSettingsService.current();
    var embeddings = aiProviderClient.embed(providerSettings, chunks.stream().map(ChunkDescriptor::text).toList()).orElse(List.of());
    embeddingChunkRepository.deleteBySourceTypeAndSourceId(sourceType, sourceId);
    List<EmbeddingChunk> persisted = new ArrayList<>();
    int index = 1;
    for (ChunkDescriptor descriptor : chunks) {
      EmbeddingChunk chunk = new EmbeddingChunk();
      chunk.setClientId(clientId);
      chunk.setSourceId(sourceId);
      chunk.setSourceType(sourceType);
      chunk.setChunkText(descriptor.text());
      chunk.setChunkIndex(descriptor.chunkIndex());
      chunk.setTokenCount(descriptor.tokenCount());
      chunk.setSourceTitle(descriptor.sourceTitle());
      chunk.setSourceSection(descriptor.sourceSection());
      chunk.setDocumentId(projection.documentId());
      chunk.setDocumentVersionId(projection.documentVersionId());
      chunk.setDocumentTypeId(projection.documentTypeId());
      chunk.setPageNumber(descriptor.pageNumber());
      chunk.setMetadataSummary(descriptor.metadataSummary());
      chunk.setPolicyNumber(projection.businessReferences().policyNumber());
      chunk.setClaimNumber(projection.businessReferences().claimNumber());
      chunk.setInsurer(projection.businessReferences().insurer());
      chunk.setPolicyType(projection.businessReferences().policyType());
      chunk.setEffectiveDate(projection.businessReferences().effectiveDate());
      chunk.setExpiryDate(projection.businessReferences().expiryDate());
      chunk.setRenewalDate(projection.businessReferences().renewalDate());
      chunk.setBrokerReference(projection.businessReferences().brokerReference());
      chunk.setExternalReference(projection.businessReferences().externalReference());
      chunk.setSourceSystem(projection.sourceSystem());
      chunk.setSecurityClassification(projection.securityClassification());
      chunk.setAclSummary(projection.aclSummary());
      chunk.setContentHash(projection.contentHash());
      chunk.setReindexVersion(projection.reindexVersion());
      chunk.setLanguage(descriptor.language());
      chunk.setEmbeddingReference(providerSettings.providerName() + ":" + providerSettings.embeddingModelName() + ":" + index++);
      if (embeddings.size() >= index && embeddings.get(index - 1) != null && !embeddings.get(index - 1).isEmpty()) {
        chunk.setEmbeddingVector(toVectorLiteral(embeddings.get(index - 1)));
      }
      persisted.add(embeddingChunkRepository.save(chunk));
    }
    return persisted;
  }

  static List<ChunkDescriptor> semanticChunks(
      List<PageSegment> pageSegments,
      String sourceTitle,
      String language,
      String sourceSection) {
    return semanticChunks(
        pageSegments,
        sourceTitle,
        language,
        sourceSection,
        new BusinessReferenceProjection(null, null, null, null, null, null, null, null, null));
  }

  static List<ChunkDescriptor> semanticChunks(
      List<PageSegment> pageSegments,
      String sourceTitle,
      String language,
      String sourceSection,
      BusinessReferenceProjection businessReferences) {
    List<ChunkDescriptor> chunks = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    String overlap = "";
    int index = 0;
    Integer currentPageNumber = null;

    List<PageSegment> safeSegments = pageSegments == null || pageSegments.isEmpty()
        ? List.of(new PageSegment(null, sourceTitle))
        : pageSegments;

    for (PageSegment segment : safeSegments) {
      String segmentText = segment.text() == null || segment.text().isBlank() ? sourceTitle : segment.text().trim();
      if (currentPageNumber == null) {
        currentPageNumber = segment.pageNumber();
      }
      for (String paragraph : splitParagraphs(segmentText)) {
        for (String sentence : splitSentences(paragraph)) {
        if (!current.isEmpty() && current.length() + sentence.length() + 1 > 900) {
          String chunkText = current.toString().trim();
          chunks.add(new ChunkDescriptor(
              chunkText,
              index++,
              approximateTokenCount(chunkText),
              sourceTitle,
              sourceSection,
              currentPageNumber,
              metadataSummary(sourceTitle, sourceSection, language, chunkText, businessReferences),
              language));
          overlap = trailingContext(chunkText);
          current = new StringBuilder(overlap);
          currentPageNumber = segment.pageNumber();
        }
        if (!current.isEmpty()) {
          current.append(' ');
        }
        current.append(sentence.trim());
      }
      }
    }
    if (!current.isEmpty()) {
      String chunkText = current.toString().trim();
      chunks.add(new ChunkDescriptor(
          chunkText,
          index,
          approximateTokenCount(chunkText),
          sourceTitle,
          sourceSection,
          currentPageNumber,
          metadataSummary(sourceTitle, sourceSection, language, chunkText, businessReferences),
          language));
    }
    if (chunks.isEmpty()) {
      chunks.add(new ChunkDescriptor(
          sourceTitle,
          0,
          approximateTokenCount(sourceTitle),
          sourceTitle,
          sourceSection,
          currentPageNumber,
          metadataSummary(sourceTitle, sourceSection, language, sourceTitle, businessReferences),
          language));
    }
    return chunks;
  }

  private static List<String> splitParagraphs(String value) {
    List<String> paragraphs = new ArrayList<>();
    for (String paragraph : value.split("\\R\\s*\\R")) {
      String trimmed = paragraph.trim();
      if (!trimmed.isBlank()) {
        paragraphs.add(trimmed);
      }
    }
    if (paragraphs.isEmpty()) {
      paragraphs.add(value.trim());
    }
    return paragraphs;
  }

  private static List<String> splitSentences(String paragraph) {
    List<String> sentences = new ArrayList<>();
    for (String sentence : paragraph.split("(?<=[.!?])\\s+")) {
      String trimmed = sentence.trim();
      if (!trimmed.isBlank()) {
        sentences.add(trimmed);
      }
    }
    if (sentences.isEmpty()) {
      sentences.add(paragraph.trim());
    }
    return sentences;
  }

  private static String trailingContext(String chunkText) {
    List<String> sentences = splitSentences(chunkText);
    if (sentences.isEmpty()) {
      return "";
    }
    return sentences.get(sentences.size() - 1);
  }

  private static int approximateTokenCount(String value) {
    if (value == null || value.isBlank()) {
      return 0;
    }
    return value.trim().split("\\s+").length;
  }

  private static String metadataSummary(
      String sourceTitle,
      String sourceSection,
      String language,
      String chunkText,
      BusinessReferenceProjection businessReferences) {
    StringBuilder builder = new StringBuilder("title=" + nullSafe(sourceTitle)
        + "; section=" + nullSafe(sourceSection)
        + "; language=" + nullSafe(language)
        + "; preview=" + (chunkText.length() <= 140 ? chunkText : chunkText.substring(0, 140).trim()));
    appendMetadata(builder, "policy_number", businessReferences.policyNumber());
    appendMetadata(builder, "claim_number", businessReferences.claimNumber());
    appendMetadata(builder, "insurer", businessReferences.insurer());
    appendMetadata(builder, "policy_type", businessReferences.policyType());
    appendMetadata(builder, "effective_date", businessReferences.effectiveDate());
    appendMetadata(builder, "expiry_date", businessReferences.expiryDate());
    appendMetadata(builder, "renewal_date", businessReferences.renewalDate());
    appendMetadata(builder, "broker_reference", businessReferences.brokerReference());
    appendMetadata(builder, "external_reference", businessReferences.externalReference());
    return builder.toString();
  }

  private static String inferLanguage(String text) {
    String normalized = nullSafe(text).toLowerCase(Locale.ROOT);
    return normalized.contains(" der ") || normalized.contains(" und ") ? "de" : "en";
  }

  private static String nullSafe(String value) {
    return value == null ? "" : value;
  }

  private static void appendMetadata(StringBuilder builder, String key, Object value) {
    if (value == null) {
      return;
    }
    builder.append("; ").append(key).append('=').append(value);
  }

  private static String toVectorLiteral(List<Double> values) {
    StringBuilder builder = new StringBuilder("[");
    for (int index = 0; index < values.size(); index++) {
      if (index > 0) {
        builder.append(',');
      }
      builder.append(values.get(index));
    }
    builder.append(']');
    return builder.toString();
  }

  static BusinessReferenceProjection businessReferencesFromMetadata(List<MetadataValue> values) {
    Map<String, String> normalized = values.stream()
        .collect(java.util.stream.Collectors.toMap(
            value -> normalizeField(value.getField().getFieldKey(), value.getField().getLabel()),
            MetadataValue::getTextValue,
            (left, right) -> left));
    return new BusinessReferenceProjection(
        firstPresent(normalized, "policy_number", "policy"),
        firstPresent(normalized, "claim_number", "claim"),
        firstPresent(normalized, "insurer", "carrier"),
        firstPresent(normalized, "policy_type", "line_of_business"),
        parseDate(firstPresent(normalized, "effective_date", "effective")),
        parseDate(firstPresent(normalized, "expiry_date", "expiration_date", "expiry")),
        parseDate(firstPresent(normalized, "renewal_date", "renewal")),
        firstPresent(normalized, "broker_reference", "broker_ref"),
        firstPresent(normalized, "external_reference", "external_system_reference", "source_system_reference"));
  }

  private IndexProjection projectionForDocumentVersion(UUID clientId, DocumentVersion version) {
    UUID documentId = version.getDocument() == null ? null : version.getDocument().getId();
    List<MetadataValue> metadataValues = documentId == null
        ? List.of()
        : metadataValueRepository.findByOwnerTypeAndOwnerId("DOCUMENT", documentId);
    BusinessReferenceProjection businessReferences = businessReferencesFromMetadata(metadataValues);
    boolean containsPii = metadataValues.stream().anyMatch(value -> value.getField().isPii());
    return new IndexProjection(
        businessReferences,
        documentId,
        version.getId(),
        version.getDocument() == null ? null : version.getDocument().getDocumentTypeId(),
        version.getDocument() == null || version.getDocument().getSource() == null ? "DOCUMENT" : version.getDocument().getSource().name(),
        containsPii ? "PII_RESTRICTED" : version.getRedactedStoragePath() != null ? "REDACTABLE" : "STANDARD",
        "clientId=" + clientId + ";sourceType=DOCUMENT;scope=CUSTOMER_KNOWLEDGE",
        version.getFileHash(),
        CURRENT_REINDEX_VERSION);
  }

  private IndexProjection projectionForAuxiliarySource(
      UUID clientId,
      String ownerType,
      UUID ownerId,
      String sourceSystem,
      String content) {
    List<MetadataValue> metadataValues = metadataValueRepository.findByOwnerTypeAndOwnerId(ownerType, ownerId);
    boolean containsPii = metadataValues.stream().anyMatch(value -> value.getField().isPii());
    return new IndexProjection(
        businessReferencesFromMetadata(metadataValues),
        null,
        null,
        null,
        sourceSystem,
        containsPii ? "PII_RESTRICTED" : "STANDARD",
        "clientId=" + clientId + ";sourceType=" + ownerType + ";scope=CUSTOMER_KNOWLEDGE",
        sha256Hex(content),
        CURRENT_REINDEX_VERSION);
  }

  private static String normalizeField(String fieldKey, String label) {
    String raw = (fieldKey == null || fieldKey.isBlank() ? label : fieldKey).toLowerCase(Locale.ROOT);
    return raw.replaceAll("[^a-z0-9]+", "_").replaceAll("_+", "_").replaceAll("^_|_$", "");
  }

  private static String firstPresent(Map<String, String> values, String... keys) {
    for (String key : keys) {
      String value = values.get(key);
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return null;
  }

  private static LocalDate parseDate(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(value.trim());
    } catch (Exception ignored) {
      return null;
    }
  }

  private static String sha256Hex(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(nullSafe(value).getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available", exception);
    }
  }

  record BusinessReferenceProjection(
      String policyNumber,
      String claimNumber,
      String insurer,
      String policyType,
      LocalDate effectiveDate,
      LocalDate expiryDate,
      LocalDate renewalDate,
      String brokerReference,
      String externalReference) {
  }

  record IndexProjection(
      BusinessReferenceProjection businessReferences,
      UUID documentId,
      UUID documentVersionId,
      UUID documentTypeId,
      String sourceSystem,
      String securityClassification,
      String aclSummary,
      String contentHash,
      int reindexVersion) {
  }

  record ChunkDescriptor(
      String text,
      int chunkIndex,
      int tokenCount,
      String sourceTitle,
      String sourceSection,
      Integer pageNumber,
      String metadataSummary,
      String language) {
  }
}
