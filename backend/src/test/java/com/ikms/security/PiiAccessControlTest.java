package com.ikms.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ikms.client.ClientContracts;
import com.ikms.security.domain.Permission;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PiiAccessControlTest {

  private PiiMaskingService piiMaskingService;
  private SecurityTrimService securityTrimService;

  @BeforeEach
  void setUp() {
    piiMaskingService = new PiiMaskingService();
    securityTrimService = new SecurityTrimService(piiMaskingService);
  }

  @Test
  void processorShouldReceiveMaskedClientProfileFields() {
    ClientContracts.ClientProfileResponse profile = new ClientContracts.ClientProfileResponse(
        UUID.fromString("11111111-1111-1111-1111-111111111111"),
        "CL-100",
        false,
        com.ikms.client.ClientType.INDIVIDUAL,
        com.ikms.client.ClientStatus.ACTIVE,
        "Alex Broker",
        "Alex Broker Legal",
        "alex@example.com",
        "+1-555-0100",
        "Alex Broker",
        Instant.parse("2026-07-10T09:00:00Z"),
        Instant.parse("2026-07-10T09:00:00Z"));

    var masked = piiMaskingService.maskClientProfile(profile, Set.of(Permission.CLIENT_VIEW));

    assertThat(masked.primaryEmail()).isEqualTo("a***@example.com");
    assertThat(masked.primaryPhone()).isEqualTo("***-***-0100");
    assertThat(masked.contactPerson()).isEqualTo("A***");
  }

  @Test
  void supervisorShouldRetainOriginalClientProfileFields() {
    ClientContracts.ClientProfileResponse profile = new ClientContracts.ClientProfileResponse(
        UUID.fromString("11111111-1111-1111-1111-111111111111"),
        "CL-100",
        false,
        com.ikms.client.ClientType.INDIVIDUAL,
        com.ikms.client.ClientStatus.ACTIVE,
        "Alex Broker",
        "Alex Broker Legal",
        "alex@example.com",
        "+1-555-0100",
        "Alex Broker",
        Instant.parse("2026-07-10T09:00:00Z"),
        Instant.parse("2026-07-10T09:00:00Z"));

    var masked = piiMaskingService.maskClientProfile(profile, Set.of(Permission.CLIENT_VIEW, Permission.VIEW_PII));

    assertThat(masked.primaryEmail()).isEqualTo("alex@example.com");
    assertThat(masked.primaryPhone()).isEqualTo("+1-555-0100");
    assertThat(masked.contactPerson()).isEqualTo("Alex Broker");
  }

  @Test
  void processorSearchAndAiContextShouldBeTrimmedRatherThanPassedThrough() {
    String input = "Call Alex at +1-555-0100 or alex@example.com";

    String search = securityTrimService.trimSearchResult(Set.of(Permission.SEARCH_CLIENT_KNOWLEDGE), input, true);
    String ai = securityTrimService.trimAiContext(Set.of(Permission.ASK_CLIENT_AI), input, true);

    assertThat(search).contains("***-***-0100");
    assertThat(search).contains("a***@example.com");
    assertThat(ai).contains("***-***-0100");
    assertThat(ai).contains("a***@example.com");
  }
}
