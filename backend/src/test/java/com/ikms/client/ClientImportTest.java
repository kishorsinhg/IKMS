package com.ikms.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ikms.audit.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClientImportTest {

  private AuditService auditService;
  private ClientImportService clientImportService;

  @BeforeEach
  void setUp() {
    auditService = mock(AuditService.class);
    clientImportService = new ClientImportService(auditService);
  }

  @Test
  void importCsvShouldFlagValidationErrorsAndDuplicateWarnings() {
    String csv = """
        clientId,displayName,email,clientType,status
        C-100,Acme Insurance,ops@acme.test,BUSINESS,ACTIVE
        C-100,Acme Duplicate,duplicate-email,UNKNOWN,PENDING
        ,Missing Id,missing@id.test,INDIVIDUAL,ACTIVE
        """;

    ClientImportService.ClientImportResult result = clientImportService.importCsv(
        "clients.csv",
        csv,
        null);

    assertThat(result.totalRows()).isEqualTo(3);
    assertThat(result.acceptedRows()).isEqualTo(1);
    assertThat(result.warningCount()).isEqualTo(1);
    assertThat(result.errorCount()).isEqualTo(4);
    assertThat(result.rows().get(1).warnings()).contains("Duplicate clientId appears multiple times in this import file.");
    assertThat(result.rows().get(1).errors()).contains(
        "email must be a valid email address.",
        "clientType must be INDIVIDUAL or BUSINESS when provided.",
        "status must be ACTIVE, INACTIVE, or ARCHIVED when provided.");
    assertThat(result.rows().get(2).errors()).contains("clientId is required.");
    verify(auditService).write(any());
  }

  @Test
  void importCsvShouldRejectMissingHeaders() {
    ClientImportService.ClientImportResult result = clientImportService.importCsv(
        "clients.csv",
        "displayName,email\nAcme Insurance,ops@acme.test\n",
        null);

    assertThat(result.fileErrors()).containsExactly("Missing required CSV headers: clientId");
    assertThat(result.rows()).isEmpty();
    assertThat(result.acceptedRows()).isZero();
    verify(auditService).write(any());
  }
}
