package com.ikms.config.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetadataFieldRepository extends JpaRepository<MetadataField, UUID> {

  Optional<MetadataField> findByFieldKeyIgnoreCase(String fieldKey);
}
