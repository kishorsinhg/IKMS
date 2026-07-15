package com.ikms.document;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class VirusScanService {

  public ScanResult scan(String filename, String mimeType, byte[] fileBytes) {
    return new ScanResult(true, "builtin-placeholder", new BigDecimal("0.9900"), null);
  }

  public record ScanResult(
      boolean clean,
      String provider,
      BigDecimal confidence,
      String detectionName) {
  }
}
