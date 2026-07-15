package com.ikms.quality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ikms.security.AppUserPrincipal;
import com.ikms.security.domain.Permission;
import com.ikms.security.domain.UserRole;
import com.ikms.security.domain.UserStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

class KnowledgeQualityControllerTest {

  private KnowledgeQualityService knowledgeQualityService;
  private KnowledgeQualityController controller;

  @BeforeEach
  void setUp() {
    knowledgeQualityService = mock(KnowledgeQualityService.class);
    controller = new KnowledgeQualityController(knowledgeQualityService);
  }

  @Test
  void customersReturnsQualitySummariesForStewardUsers() {
    UUID clientId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    when(knowledgeQualityService.listCustomers(isNull(), anyBoolean())).thenReturn(
        new KnowledgeQualityContracts.KnowledgeQualityCustomerListResponse(List.of(
            new KnowledgeQualityContracts.CustomerKnowledgeQualitySummaryResponse(
                clientId,
                "Acme Logistics",
                "C-100",
                new BigDecimal("0.8200"),
                "READY",
                1,
                1,
                Instant.parse("2026-07-15T12:00:00Z"),
                List.of(new KnowledgeQualityContracts.QualityDimensionScoreResponse(
                    "business-references",
                    "Business Reference quality",
                    new BigDecimal("0.8100"),
                    "Business Reference Fields remain searchable customer knowledge metadata.")),
                List.of("Review duplicate claim reference metadata.")))));

    KnowledgeQualityContracts.KnowledgeQualityCustomerListResponse response =
        controller.customers(null, false, stewardAuthentication());

    assertThat(response.customers()).hasSize(1);
    assertThat(response.customers().get(0).customerName()).isEqualTo("Acme Logistics");
    assertThat(response.customers().get(0).dimensions().get(0).label()).isEqualTo("Business Reference quality");
    verify(knowledgeQualityService).listCustomers(eq(null), eq(false));
  }

  @Test
  void customersRejectsUsersWithoutStewardPermission() {
    assertThatThrownBy(() -> controller.customers(null, false, nonStewardAuthentication()))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("MANAGE_CONFIGURATION");
  }

  private Authentication stewardAuthentication() {
    AppUserPrincipal principal = new AppUserPrincipal(
        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
        "admin",
        "password",
        "Administrator",
        "admin@example.com",
        UserStatus.ACTIVE,
        Set.of(UserRole.ADMINISTRATOR),
        Set.of(Permission.MANAGE_CONFIGURATION),
        List.of());
    return new TestingAuthenticationToken(principal, null);
  }

  private Authentication nonStewardAuthentication() {
    AppUserPrincipal principal = new AppUserPrincipal(
        UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
        "processor",
        "password",
        "Processor",
        "processor@example.com",
        UserStatus.ACTIVE,
        Set.of(UserRole.PROCESSOR),
        Set.of(Permission.SEARCH_CLIENT_KNOWLEDGE),
        List.of());
    return new TestingAuthenticationToken(principal, null);
  }
}
