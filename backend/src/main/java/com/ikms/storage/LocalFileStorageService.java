package com.ikms.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class LocalFileStorageService implements FileStorageService {

  private final Path root;

  public LocalFileStorageService(@Value("${ikms.storage.root:${java.io.tmpdir}/ikms-storage}") String rootPath) {
    this.root = Path.of(rootPath).toAbsolutePath().normalize();
  }

  @Override
  public StoredFile store(StoreRequest request) {
    UUID id = UUID.randomUUID();
    String sanitizedFilename = sanitize(request.originalFilename());
    String storageKey = id + "/" + sanitizedFilename;
    Path target = root.resolve(storageKey);
    try {
      Files.createDirectories(target.getParent());
      try (InputStream inputStream = request.content()) {
        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to store file.", exception);
    }
    return new StoredFile(
        id,
        storageKey,
        request.originalFilename(),
        request.contentType(),
        request.variant(),
        request.contentLength());
  }

  @Override
  public Resource load(String storageKey) {
    return new FileSystemResource(root.resolve(storageKey));
  }

  @Override
  public void delete(String storageKey) {
    try {
      Files.deleteIfExists(root.resolve(storageKey));
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to delete file.", exception);
    }
  }

  private static String sanitize(String filename) {
    return filename.replaceAll("[^A-Za-z0-9._-]", "_");
  }
}
