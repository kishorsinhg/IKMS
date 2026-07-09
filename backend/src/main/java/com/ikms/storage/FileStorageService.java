package com.ikms.storage;

import java.io.InputStream;
import java.util.UUID;
import org.springframework.core.io.Resource;

public interface FileStorageService {

  StoredFile store(StoreRequest request);

  Resource load(String storageKey);

  void delete(String storageKey);

  record StoreRequest(
      String originalFilename,
      String contentType,
      FileVariant variant,
      long contentLength,
      InputStream content) {
  }

  record StoredFile(
      UUID id,
      String storageKey,
      String originalFilename,
      String contentType,
      FileVariant variant,
      long contentLength) {
  }

  enum FileVariant {
    ORIGINAL,
    REDACTED
  }
}
