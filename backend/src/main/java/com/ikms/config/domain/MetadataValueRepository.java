package com.ikms.config.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetadataValueRepository extends JpaRepository<MetadataValue, UUID> {

  List<MetadataValue> findByOwnerTypeAndOwnerId(String ownerType, UUID ownerId);

  Optional<MetadataValue> findByOwnerTypeAndOwnerIdAndField_Id(String ownerType, UUID ownerId, UUID fieldId);

  List<MetadataValue> findByOwnerTypeAndTextValueContainingIgnoreCase(String ownerType, String textValue);

  List<MetadataValue> findByOwnerTypeAndField_FieldKeyIgnoreCaseAndTextValueIgnoreCase(
      String ownerType,
      String fieldKey,
      String textValue);

  void deleteByOwnerTypeAndOwnerId(String ownerType, UUID ownerId);
}
