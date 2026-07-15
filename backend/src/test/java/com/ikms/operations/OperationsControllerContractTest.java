package com.ikms.operations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ikms.common.api.GlobalExceptionHandler;
import com.ikms.governance.InformationClassification;
import com.ikms.security.AppUserPrincipal;
import com.ikms.security.domain.Permission;
import com.ikms.security.domain.UserRole;
import com.ikms.security.domain.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OperationsControllerContractTest {

  private MockMvc mockMvc;
  private OperationsService operationsService;
  private AlertDefinitionService alertDefinitionService;

  @BeforeEach
  void setUp() {
    operationsService = Mockito.mock(OperationsService.class);
    alertDefinitionService = Mockito.mock(AlertDefinitionService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new OperationsController(operationsService, alertDefinitionService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void shouldListOperationsJobsForAuthorizedUser() throws Exception {
    when(operationsService.listJobs()).thenReturn(List.of(new OperationsContracts.JobResponse(
        UUID.fromString("11111111-1111-1111-1111-111111111111"),
        "FULL_PROJECTION_REBUILD",
        UUID.fromString("22222222-2222-2222-2222-222222222222"),
        Instant.parse("2026-07-15T10:00:00Z"),
        Instant.parse("2026-07-15T10:01:00Z"),
        null,
        null,
        "RUNNING",
        42,
        null,
        0,
        "REINDEX",
        "PROJECTION",
        "ALL",
        100,
        false,
        java.util.Map.of("scope", "full"))));

    mockMvc.perform(get("/api/operations/jobs")
            .principal(authentication(Set.of(Permission.VIEW_OPERATIONS))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].jobType").value("FULL_PROJECTION_REBUILD"))
        .andExpect(jsonPath("$[0].progress").value(42));
  }

  @Test
  void shouldPauseQueueForOperator() throws Exception {
    when(operationsService.pauseQueue(eq("REINDEX"), any())).thenReturn(new OperationsContracts.QueueResponse(
        "REINDEX",
        "Reindex Queue",
        "PAUSED",
        true,
        2,
        0,
        0,
        Instant.parse("2026-07-15T10:03:00Z"),
        "Controls reindex and projection rebuild jobs."));

    mockMvc.perform(post("/api/operations/queues/REINDEX/pause")
            .principal(authentication(Set.of(Permission.MANAGE_OPERATIONS))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paused").value(true))
        .andExpect(jsonPath("$.queueName").value("Reindex Queue"));

    verify(operationsService).pauseQueue(eq("REINDEX"), any());
  }

  @Test
  void shouldRejectHealthWhenPermissionMissing() throws Exception {
    mockMvc.perform(get("/api/operations/health")
            .principal(authentication(Set.of(Permission.VIEW_OPERATIONS)))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldListAlertDefinitionsForAuthorizedUser() throws Exception {
    when(alertDefinitionService.definitions()).thenReturn(List.of(
        new OperationsContracts.AlertDefinitionResponse(
            "ALT-001",
            "HIGH",
            "AI",
            ">= 3 failures",
            "10 minutes",
            "ENGINEERING",
            "Inspect the provider and fallback path.")));

    mockMvc.perform(get("/api/operations/alerts")
            .principal(authentication(Set.of(Permission.VIEW_OPERATIONS))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].alertId").value("ALT-001"))
        .andExpect(jsonPath("$[0].category").value("AI"));
  }

  private TestingAuthenticationToken authentication(Set<Permission> permissions) {
    AppUserPrincipal principal = new AppUserPrincipal(
        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
        "admin",
        "password",
        "Admin User",
        "admin@ikms.local",
        UserStatus.ACTIVE,
        "Operations",
        "Operations",
        "IN",
        "IN",
        "Operations",
        "Administrator",
        InformationClassification.INTERNAL,
        Set.of(UserRole.ADMINISTRATOR),
        permissions,
        permissions.stream().map(permission -> new SimpleGrantedAuthority(permission.name())).toList());
    return new TestingAuthenticationToken(principal, null, permissions.stream().map(Enum::name).toArray(String[]::new));
  }
}
