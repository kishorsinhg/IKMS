package com.ikms.ai;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikms.ai.orchestration.EnterpriseAiContracts;
import com.ikms.ai.orchestration.EnterpriseAiOperation;
import com.ikms.ai.orchestration.EnterpriseAiOrchestrationService;
import com.ikms.ai.orchestration.IntentDetectionService;
import com.ikms.ai.orchestration.QueryPlanningService;
import com.ikms.common.api.GlobalExceptionHandler;
import com.ikms.search.ClientSearchService;
import com.ikms.search.SearchContracts;
import com.ikms.security.AppUserPrincipal;
import com.ikms.security.domain.Permission;
import com.ikms.security.domain.UserRole;
import com.ikms.security.domain.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class EnterpriseKnowledgeControllerTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;
  private AppUserPrincipal principal;

  private IntentDetectionService intentDetectionService;
  private QueryPlanningService queryPlanningService;
  private ClientSearchService clientSearchService;
  private EnterpriseAiOrchestrationService enterpriseAiOrchestrationService;
  private AiCitationRecordRepository aiCitationRecordRepository;
  private AiConversationMessageRepository aiConversationMessageRepository;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().findAndRegisterModules();
    principal = new AppUserPrincipal(
        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
        "broker",
        "secret",
        "Broker User",
        "broker@example.com",
        UserStatus.ACTIVE,
        Set.of(UserRole.SUPERVISOR),
        Set.of(Permission.SEARCH_CLIENT_KNOWLEDGE, Permission.ASK_CLIENT_AI),
        List.of());

    intentDetectionService = mock(IntentDetectionService.class);
    queryPlanningService = mock(QueryPlanningService.class);
    clientSearchService = mock(ClientSearchService.class);
    enterpriseAiOrchestrationService = mock(EnterpriseAiOrchestrationService.class);
    aiCitationRecordRepository = mock(AiCitationRecordRepository.class);
    aiConversationMessageRepository = mock(AiConversationMessageRepository.class);

    mockMvc = MockMvcBuilders.standaloneSetup(new EnterpriseKnowledgeController(
            intentDetectionService,
            queryPlanningService,
            clientSearchService,
            enterpriseAiOrchestrationService,
            aiCitationRecordRepository,
            aiConversationMessageRepository))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void searchKnowledgeShouldReturnBusinessReferenceFilters() throws Exception {
    EnterpriseAiContracts.QueryPlan plan = queryPlan("policy renewal", EnterpriseAiContracts.QueryScope.CUSTOMER);
    when(intentDetectionService.detect(any())).thenReturn(new EnterpriseAiContracts.DetectedIntent(
        EnterpriseAiOperation.SEARCH,
        "policy renewal",
        "GROUND",
        false));
    when(queryPlanningService.plan(any(), any())).thenReturn(plan);
    when(clientSearchService.searchDetailed(any())).thenReturn(new ClientSearchService.SearchOutcome(
        List.of(new SearchContracts.SearchResultResponse(
            "DOCUMENT",
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            "Renewal Schedule",
            "Northwind Mutual renewal discussion",
            "Document: Renewal Schedule",
            4,
            "document-version",
            "HYBRID_VECTOR",
            "HIGH",
            Instant.parse("2026-07-15T00:00:00Z"))),
        "HYBRID_VECTOR",
        List.of()));

    mockMvc.perform(get("/api/search/knowledge")
            .principal(authentication())
            .param("query", "policy renewal")
            .param("customerId", "11111111-1111-1111-1111-111111111111")
            .param("policyNumber", "POL-12345")
            .param("claimNumber", "CLM-9988")
            .param("insurer", "Northwind Mutual"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.queryPlan.businessReferenceFields.policyNumber").value("POL-12345"))
        .andExpect(jsonPath("$.results[0].sourceType").value("DOCUMENT"));
  }

  @Test
  void askShouldReturnGroundedEnterpriseResponse() throws Exception {
    EnterpriseAiContracts.QueryPlan plan = queryPlan("Show correspondence for claim CLM-9988", EnterpriseAiContracts.QueryScope.GLOBAL);
    when(intentDetectionService.detect(any())).thenReturn(new EnterpriseAiContracts.DetectedIntent(
        EnterpriseAiOperation.ASK,
        "Show correspondence for claim CLM-9988",
        "GROUND",
        false));
    when(queryPlanningService.plan(any(), any())).thenReturn(plan);
    when(enterpriseAiOrchestrationService.orchestrate(
        eq(null),
        eq(EnterpriseAiOperation.ASK),
        eq("Show correspondence for claim CLM-9988"),
        any(),
        any(),
        eq(null),
        eq(List.of()),
        any())).thenReturn(aiResponse("Ready", "Grounded answer", true, List.of("Warning")));

    mockMvc.perform(post("/api/ask")
            .principal(authentication())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(new AiContracts.GlobalAskRequest(
                "Show correspondence for claim CLM-9988",
                null,
                Map.of("claimNumber", "CLM-9988")))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Answered"))
        .andExpect(jsonPath("$.groundingStatus").value("GROUNDED"))
        .andExpect(jsonPath("$.citations[0].supportingAttributes.claimNumber").value("CLM-9988"));
  }

  @Test
  void evidenceShouldReturnCitationBackedExpansion() throws Exception {
    AiCitationRecord record = new AiCitationRecord();
    record.setInteractionId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
    record.setSourceType("DOCUMENT");
    record.setSourceId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    record.setTitle("Insurer Correspondence");
    record.setExcerpt("Policy POL-12345 and claim CLM-9988");
    when(aiCitationRecordRepository.findByInteractionIdOrderByCreatedAtAsc(record.getInteractionId())).thenReturn(List.of(record));

    mockMvc.perform(get("/api/ai/interactions/{interactionId}/evidence", "33333333-3333-3333-3333-333333333333")
            .principal(authentication()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.evidence[0].supportingAttributes.policyNumber").value("POL-12345"))
        .andExpect(jsonPath("$.evidence[0].supportingAttributes.claimNumber").value("CLM-9988"));
  }

  @Test
  void continueConversationShouldReturnHistoryCountAndPlan() throws Exception {
    when(intentDetectionService.detect(any())).thenReturn(new EnterpriseAiContracts.DetectedIntent(
        EnterpriseAiOperation.ASK,
        "Compare with the previous version",
        "COMPARE",
        true));
    when(queryPlanningService.plan(any(), any())).thenReturn(queryPlan("Compare with the previous version", EnterpriseAiContracts.QueryScope.GLOBAL));
    when(aiConversationMessageRepository.countByConversationId(UUID.fromString("44444444-4444-4444-4444-444444444444"))).thenReturn(4L);

    mockMvc.perform(post("/api/ai/conversations/{conversationId}/continue", "44444444-4444-4444-4444-444444444444")
            .principal(authentication())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(new AiContracts.ConversationContinueRequest(
                "Compare with the previous version",
                List.of(),
                Map.of()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.historyCount").value(4))
        .andExpect(jsonPath("$.queryPlan.versionPreference").value("PREVIOUS_VERSION"));
  }

  @Test
  void streamShouldReturnEventList() throws Exception {
    when(enterpriseAiOrchestrationService.orchestrate(
        eq(null),
        eq(EnterpriseAiOperation.ASK),
        eq("Summarize this customer"),
        any(),
        any(),
        eq(null),
        eq(List.of()),
        any())).thenReturn(aiResponse("Ready", "Customer summary", true, List.of()));

    mockMvc.perform(post("/api/ai/stream")
            .principal(authentication())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(new AiContracts.StreamRequest(
                "Summarize this customer",
                null,
                null,
                List.of(),
                Map.of()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.streaming").value(true))
        .andExpect(jsonPath("$.events[0].type").value("start"))
        .andExpect(jsonPath("$.events[1].type").value("delta"));
  }

  private EnterpriseAiContracts.QueryPlan queryPlan(String query, EnterpriseAiContracts.QueryScope scope) {
    return new EnterpriseAiContracts.QueryPlan(
        EnterpriseAiOperation.ASK,
        query,
        "GROUND",
        scope,
        List.of("LEXICAL", "VECTOR"),
        List.of(EnterpriseAiContracts.SourceType.DOCUMENT),
        List.of(),
        new EnterpriseAiContracts.QueryDateRange(null, null),
        new EnterpriseAiContracts.BusinessReferenceFields("POL-12345", "CLM-9988", "Northwind Mutual", null, null, null, null, null, null),
        5,
        2,
        2000,
        20,
        EnterpriseAiContracts.SortOrder.RELEVANCE,
        EnterpriseAiContracts.VersionPreference.PREVIOUS_VERSION,
        EnterpriseAiContracts.EvidenceGranularity.CHUNK,
        List.of());
  }

  private EnterpriseAiContracts.EnterpriseAiResponse aiResponse(
      String status,
      String answer,
      boolean grounded,
      List<String> warnings) {
    return new EnterpriseAiContracts.EnterpriseAiResponse(
        EnterpriseAiOperation.ASK,
        UUID.fromString("44444444-4444-4444-4444-444444444444"),
        UUID.fromString("55555555-5555-5555-5555-555555555555"),
        status,
        answer,
        List.of(new EnterpriseAiContracts.CitationReference(
            "DOCUMENT",
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            "Insurer Correspondence",
            "Claim CLM-9988",
            4,
            null,
            "document-version",
            "HIGH",
            "document:22222222-2222-2222-2222-222222222222:page:4",
            "HYBRID_VECTOR")),
        List.of(),
        List.of(),
        warnings,
        new EnterpriseAiContracts.MetricsSnapshot(100L, 1, "HYBRID_VECTOR", false),
        new EnterpriseAiContracts.GroundingValidation(grounded, grounded ? 0.98d : 0d, grounded ? 1d : 0d, warnings),
        Map.of(
            "guardrails", Map.of("restrictedContentNotice", ""),
            "streamEvents", List.of("start", "delta", "complete")));
  }

  private UsernamePasswordAuthenticationToken authentication() {
    return new UsernamePasswordAuthenticationToken(principal, principal.password(), principal.authorities());
  }
}
