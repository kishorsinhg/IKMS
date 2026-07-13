package com.ikms.security;

import com.ikms.config.domain.MetadataValueRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ContentSensitivityService {

  private final MetadataValueRepository metadataValueRepository;

  public ContentSensitivityService(MetadataValueRepository metadataValueRepository) {
    this.metadataValueRepository = metadataValueRepository;
  }

  public boolean documentContainsPii(UUID documentId) {
    return ownerContainsPii("DOCUMENT", documentId);
  }

  public boolean emailContainsPii(UUID emailId) {
    return ownerContainsPii("EMAIL", emailId);
  }

  public boolean noteContainsPii(UUID noteId) {
    return ownerContainsPii("NOTE", noteId);
  }

  private boolean ownerContainsPii(String ownerType, UUID ownerId) {
    return metadataValueRepository.findByOwnerTypeAndOwnerId(ownerType, ownerId).stream()
        .anyMatch(value -> value.getField().isPii());
  }
}
