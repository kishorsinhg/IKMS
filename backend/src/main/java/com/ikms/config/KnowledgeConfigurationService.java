package com.ikms.config;

import com.ikms.audit.AuditService;
import com.ikms.audit.AuditService.AuditEvent;
import com.ikms.audit.AuditService.AuditOutcome;
import com.ikms.config.domain.DocumentType;
import com.ikms.config.domain.DocumentTypeRepository;
import com.ikms.config.domain.MetadataField;
import com.ikms.config.domain.MetadataFieldRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class KnowledgeConfigurationService {

  private final DocumentTypeRepository documentTypeRepository;
  private final MetadataFieldRepository metadataFieldRepository;
  private final AuditService auditService;

  public KnowledgeConfigurationService(
      DocumentTypeRepository documentTypeRepository,
      MetadataFieldRepository metadataFieldRepository,
      AuditService auditService) {
    this.documentTypeRepository = documentTypeRepository;
    this.metadataFieldRepository = metadataFieldRepository;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public List<AdminConfigurationContracts.DocumentTypeResponse> listDocumentTypes() {
    return documentTypeRepository.findAll().stream().map(this::toResponse).toList();
  }

  public AdminConfigurationContracts.DocumentTypeResponse createDocumentType(AdminConfigurationContracts.DocumentTypeRequest request, UUID actorUserId) {
    DocumentType item = new DocumentType();
    apply(item, request);
    DocumentType saved = documentTypeRepository.save(item);
    audit("DOCUMENT_TYPE_CREATED", actorUserId, saved.getId().toString(), Map.of("name", saved.getName()));
    return toResponse(saved);
  }

  public AdminConfigurationContracts.DocumentTypeResponse updateDocumentType(UUID id, AdminConfigurationContracts.DocumentTypeRequest request, UUID actorUserId) {
    DocumentType item = documentTypeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Document type not found: " + id));
    apply(item, request);
    DocumentType saved = documentTypeRepository.save(item);
    audit("DOCUMENT_TYPE_UPDATED", actorUserId, saved.getId().toString(), Map.of("name", saved.getName()));
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<AdminConfigurationContracts.MetadataFieldResponse> listMetadataFields() {
    return metadataFieldRepository.findAll().stream().map(this::toResponse).toList();
  }

  public AdminConfigurationContracts.MetadataFieldResponse createMetadataField(AdminConfigurationContracts.MetadataFieldRequest request, UUID actorUserId) {
    MetadataField item = new MetadataField();
    apply(item, request);
    MetadataField saved = metadataFieldRepository.save(item);
    audit("METADATA_FIELD_CREATED", actorUserId, saved.getId().toString(), Map.of("fieldKey", saved.getFieldKey(), "pii", Boolean.toString(saved.isPii())));
    return toResponse(saved);
  }

  public AdminConfigurationContracts.MetadataFieldResponse updateMetadataField(UUID id, AdminConfigurationContracts.MetadataFieldRequest request, UUID actorUserId) {
    MetadataField item = metadataFieldRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Metadata field not found: " + id));
    apply(item, request);
    MetadataField saved = metadataFieldRepository.save(item);
    audit("METADATA_FIELD_UPDATED", actorUserId, saved.getId().toString(), Map.of("fieldKey", saved.getFieldKey(), "pii", Boolean.toString(saved.isPii())));
    return toResponse(saved);
  }

  private void apply(DocumentType item, AdminConfigurationContracts.DocumentTypeRequest request) {
    item.setName(request.name().trim());
    item.setDescription(normalize(request.description()));
    item.setActive(request.active());
  }

  private void apply(MetadataField item, AdminConfigurationContracts.MetadataFieldRequest request) {
    item.setFieldKey(request.fieldKey().trim());
    item.setLabel(request.label().trim());
    item.setPii(request.pii());
    item.setActive(request.active());
  }

  private AdminConfigurationContracts.DocumentTypeResponse toResponse(DocumentType item) {
    return new AdminConfigurationContracts.DocumentTypeResponse(item.getId(), item.getName(), item.getDescription(), item.isActive(), item.getCreatedAt());
  }

  private AdminConfigurationContracts.MetadataFieldResponse toResponse(MetadataField item) {
    return new AdminConfigurationContracts.MetadataFieldResponse(item.getId(), item.getFieldKey(), item.getLabel(), item.isPii(), item.isActive(), item.getCreatedAt());
  }

  private void audit(String action, UUID actorUserId, String targetId, Map<String, String> details) {
    auditService.write(new AuditEvent(Instant.now(), "CONFIG", action, AuditOutcome.SUCCESS, actorUserId, null, "Configuration", targetId, false, details));
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
