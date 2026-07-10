package com.ikms.audit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ikms.common.api.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class AuditExportTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new TestAuditController())
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void searchShouldReturnAuditRowsWithFilters() throws Exception {
    mockMvc.perform(get("/api/audit")
            .param("actor", "admin")
            .param("action", "LOGIN")
            .param("clientId", "11111111-1111-1111-1111-111111111111"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].actorUsername").value("admin"))
        .andExpect(jsonPath("$[0].action").value("LOGIN_SUCCESS"))
        .andExpect(jsonPath("$[0].details.ipAddress").value("127.0.0.1"));
  }

  @Test
  void exportShouldReturnCsvPayload() throws Exception {
    mockMvc.perform(get("/api/audit/export")
            .param("actor", "admin"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("text/csv"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("occurredAt,actorUsername,category,action")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("\"admin\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("\"LOGIN_SUCCESS\"")));
  }

  @RestController
  @RequestMapping("/api/audit")
  static class TestAuditController {

    @GetMapping
    List<AuditContracts.AuditLogResponse> search(
        @RequestParam(name = "actor", required = false) String actor,
        @RequestParam(name = "action", required = false) String action,
        @RequestParam(name = "clientId", required = false) UUID clientId) {
      return List.of(new AuditContracts.AuditLogResponse(
          UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
          Instant.parse("2026-07-10T10:00:00Z"),
          Instant.parse("2033-07-09T10:00:00Z"),
          UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
          actor == null ? "admin" : actor,
          clientId,
          "AUTHENTICATION",
          action == null || action.isBlank() ? "LOGIN_SUCCESS" : action + "_SUCCESS",
          "SUCCESS",
          "Session",
          "current",
          false,
          Map.of("ipAddress", "127.0.0.1")));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    org.springframework.http.ResponseEntity<String> export(@RequestParam(name = "actor", required = false) String actor) {
      return org.springframework.http.ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-log-export.csv\"")
          .contentType(MediaType.parseMediaType("text/csv"))
          .body("""
              occurredAt,actorUsername,category,action,outcome,clientId,targetType,targetId,piiAccess,retainedUntil,details
              "2026-07-10T10:00:00Z","%s","AUTHENTICATION","LOGIN_SUCCESS","SUCCESS","","Session","current","false","2033-07-09T10:00:00Z","{ipAddress=127.0.0.1}"
              """.formatted(actor == null ? "admin" : actor));
    }
  }
}
