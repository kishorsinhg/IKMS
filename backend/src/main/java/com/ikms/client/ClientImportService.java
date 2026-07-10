package com.ikms.client;

import com.ikms.audit.AuditService;
import com.ikms.audit.AuditService.AuditEvent;
import com.ikms.audit.AuditService.AuditOutcome;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ClientImportService {

  private static final Set<String> REQUIRED_HEADERS = Set.of("clientId", "displayName");

  private final AuditService auditService;

  public ClientImportService(AuditService auditService) {
    this.auditService = auditService;
  }

  public ClientImportResult importCsv(String filename, String csvContent, UUID actorUserId) {
    List<String> fileErrors = new ArrayList<>();
    List<ClientImportRowResult> rows = new ArrayList<>();

    if (csvContent == null || csvContent.isBlank()) {
      fileErrors.add("CSV file is empty.");
      return complete(filename, actorUserId, fileErrors, rows);
    }

    List<String> lines = csvContent.lines()
        .map(String::trim)
        .filter(line -> !line.isEmpty())
        .toList();

    if (lines.isEmpty()) {
      fileErrors.add("CSV file is empty.");
      return complete(filename, actorUserId, fileErrors, rows);
    }

    List<String> headers = parseCsvLine(lines.getFirst());
    Map<String, Integer> headerIndexes = new LinkedHashMap<>();
    for (int index = 0; index < headers.size(); index++) {
      headerIndexes.put(headers.get(index).trim(), index);
    }

    List<String> missingHeaders = REQUIRED_HEADERS.stream()
        .filter(header -> !headerIndexes.containsKey(header))
        .sorted()
        .toList();
    if (!missingHeaders.isEmpty()) {
      fileErrors.add("Missing required CSV headers: " + String.join(", ", missingHeaders));
      return complete(filename, actorUserId, fileErrors, rows);
    }

    Set<String> seenClientIds = new LinkedHashSet<>();
    for (int lineNumber = 2; lineNumber <= lines.size(); lineNumber++) {
      List<String> values = parseCsvLine(lines.get(lineNumber - 1));
      String clientId = valueFor(values, headerIndexes, "clientId");
      String displayName = valueFor(values, headerIndexes, "displayName");
      String email = valueFor(values, headerIndexes, "email");
      String clientType = valueFor(values, headerIndexes, "clientType");
      String status = valueFor(values, headerIndexes, "status");

      List<String> warnings = new ArrayList<>();
      List<String> errors = new ArrayList<>();

      if (clientId.isBlank()) {
        errors.add("clientId is required.");
      }
      if (displayName.isBlank()) {
        errors.add("displayName is required.");
      }
      if (!email.isBlank() && !isLikelyEmail(email)) {
        errors.add("email must be a valid email address.");
      }
      if (!clientType.isBlank() && !Set.of("INDIVIDUAL", "BUSINESS").contains(clientType.toUpperCase(Locale.ROOT))) {
        errors.add("clientType must be INDIVIDUAL or BUSINESS when provided.");
      }
      if (!status.isBlank() && !Set.of("ACTIVE", "INACTIVE", "ARCHIVED").contains(status.toUpperCase(Locale.ROOT))) {
        errors.add("status must be ACTIVE, INACTIVE, or ARCHIVED when provided.");
      }

      String normalizedClientId = clientId.trim().toUpperCase(Locale.ROOT);
      if (!clientId.isBlank() && !seenClientIds.add(normalizedClientId)) {
        warnings.add("Duplicate clientId appears multiple times in this import file.");
      }

      rows.add(new ClientImportRowResult(
          lineNumber,
          clientId,
          displayName,
          email,
          clientType,
          status,
          warnings,
          errors,
          errors.isEmpty()));
    }

    return complete(filename, actorUserId, fileErrors, rows);
  }

  private ClientImportResult complete(
      String filename,
      UUID actorUserId,
      List<String> fileErrors,
      List<ClientImportRowResult> rows) {
    int acceptedRows = (int) rows.stream().filter(ClientImportRowResult::accepted).count();
    int warningCount = rows.stream().mapToInt(row -> row.warnings().size()).sum();
    int errorCount = fileErrors.size() + rows.stream().mapToInt(row -> row.errors().size()).sum();
    AuditOutcome outcome = acceptedRows > 0 ? AuditOutcome.SUCCESS : AuditOutcome.FAILURE;

    auditService.write(new AuditEvent(
        Instant.now(),
        "CLIENT",
        "CLIENT_IMPORT",
        outcome,
        actorUserId,
        null,
        "ClientImport",
        filename,
        false,
        Map.of(
            "acceptedRows", Integer.toString(acceptedRows),
            "errorCount", Integer.toString(errorCount),
            "warningCount", Integer.toString(warningCount),
            "totalRows", Integer.toString(rows.size()))));

    return new ClientImportResult(filename, rows.size(), acceptedRows, warningCount, errorCount, fileErrors, rows);
  }

  private static String valueFor(List<String> values, Map<String, Integer> headerIndexes, String header) {
    Integer index = headerIndexes.get(header);
    if (index == null || index >= values.size()) {
      return "";
    }
    return values.get(index).trim();
  }

  private static boolean isLikelyEmail(String value) {
    int atIndex = value.indexOf('@');
    int dotIndex = value.lastIndexOf('.');
    return atIndex > 0 && dotIndex > atIndex + 1 && dotIndex < value.length() - 1;
  }

  static List<String> parseCsvLine(String line) {
    List<String> values = new ArrayList<>();
    StringBuilder currentValue = new StringBuilder();
    boolean inQuotes = false;

    for (int index = 0; index < line.length(); index++) {
      char character = line.charAt(index);
      if (character == '"') {
        if (inQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
          currentValue.append('"');
          index++;
        } else {
          inQuotes = !inQuotes;
        }
      } else if (character == ',' && !inQuotes) {
        values.add(currentValue.toString());
        currentValue.setLength(0);
      } else {
        currentValue.append(character);
      }
    }

    values.add(currentValue.toString());
    return values;
  }

  public record ClientImportResult(
      String filename,
      int totalRows,
      int acceptedRows,
      int warningCount,
      int errorCount,
      List<String> fileErrors,
      List<ClientImportRowResult> rows) {

    public ClientImportResult {
      fileErrors = List.copyOf(fileErrors);
      rows = List.copyOf(rows);
    }
  }

  public record ClientImportRowResult(
      int lineNumber,
      String clientId,
      String displayName,
      String email,
      String clientType,
      String status,
      List<String> warnings,
      List<String> errors,
      boolean accepted) {

    public ClientImportRowResult {
      warnings = List.copyOf(warnings);
      errors = List.copyOf(errors);
    }
  }
}
