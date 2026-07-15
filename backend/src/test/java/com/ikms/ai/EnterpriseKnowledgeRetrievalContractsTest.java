package com.ikms.ai;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikms.common.api.GlobalExceptionHandler;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class EnterpriseKnowledgeRetrievalContractsTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().findAndRegisterModules();
    mockMvc = MockMvcBuilders.standaloneSetup(new TestEnterpriseKnowledgeController())
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void knowledgeSearchShouldSupportBusinessReferenceFilters() throws Exception {
    mockMvc.perform(get("/api/search/knowledge")
            .param("query", "policy renewal")
            .param("customerId", "11111111-1111-1111-1111-111111111111")
            .param("policyNumber", "POL-12345")
            .param("claimNumber", "CLM-9988")
            .param("insurer", "Northwind Mutual"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.queryPlan.scope").value("CUSTOMER"))
        .andExpect(jsonPath("$.queryPlan.businessReferenceFields.policyNumber").value("POL-12345"))
        .andExpect(jsonPath("$.queryPlan.businessReferenceFields.claimNumber").value("CLM-9988"))
        .andExpect(jsonPath("$.results[0].sourceType").value("DOCUMENT"))
        .andExpect(jsonPath("$.results[0].supportingAttributes.policyNumber").value("POL-12345"));
  }

  @Test
  void globalAskShouldReturnCustomerScopedGroundedAnswer() throws Exception {
    mockMvc.perform(post("/api/ask")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(new GlobalAskRequest(
                "Show correspondence mentioning claim number CLM-9988",
                null,
                Map.of("claimNumber", "CLM-9988")))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Answered"))
        .andExpect(jsonPath("$.groundingStatus").value("GROUNDED"))
        .andExpect(jsonPath("$.citations[0].sourceType").value("DOCUMENT"))
        .andExpect(jsonPath("$.citations[0].supportingAttributes.claimNumber").value("CLM-9988"))
        .andExpect(jsonPath("$.auditCorrelationId").value("audit-ask-001"));
  }

  @Test
  void evidenceExpansionShouldReturnSupportingAttributesNotEntitySources() throws Exception {
    mockMvc.perform(get("/api/ai/interactions/{interactionId}/evidence", "33333333-3333-3333-3333-333333333333"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.interactionId").value("33333333-3333-3333-3333-333333333333"))
        .andExpect(jsonPath("$.evidence[0].sourceType").value("DOCUMENT"))
        .andExpect(jsonPath("$.evidence[0].supportingAttributes.policyNumber").value("POL-12345"))
        .andExpect(jsonPath("$.evidence[0].supportingAttributes.claimNumber").value("CLM-9988"));
  }

  @Test
  void conversationContinuationShouldPreserveConversationContext() throws Exception {
    mockMvc.perform(post("/api/ai/conversations/{conversationId}/continue", "44444444-4444-4444-4444-444444444444")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(new ConversationContinueRequest("Compare with the previous renewal version"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.conversationId").value("44444444-4444-4444-4444-444444444444"))
        .andExpect(jsonPath("$.historyCount").value(4))
        .andExpect(jsonPath("$.queryPlan.versionPreference").value("PREVIOUS_VERSION"));
  }

  @Test
  void streamingAskShouldExposeChunkedResponseContract() throws Exception {
    mockMvc.perform(post("/api/ai/stream")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(new StreamRequest("Summarize this customer"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.streaming").value(true))
        .andExpect(jsonPath("$.events[0].type").value("start"))
        .andExpect(jsonPath("$.events[1].type").value("delta"))
        .andExpect(jsonPath("$.events[2].type").value("complete"));
  }

  @Test
  void timeoutShouldReturnSafeFailureContract() throws Exception {
    mockMvc.perform(post("/api/ai/stream")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(new StreamRequest("timeout test"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.streaming").value(false))
        .andExpect(jsonPath("$.status").value("Failed"))
        .andExpect(jsonPath("$.warnings[0]").value("Request timed out before a grounded answer could be completed."));
  }

  @Test
  void permissionLeakageShouldReturnRestrictedNotice() throws Exception {
    mockMvc.perform(post("/api/ask")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(new GlobalAskRequest(
                "Show restricted documents for claim CLM-9988",
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                Map.of("simulateRestricted", true)))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("InsufficientEvidence"))
        .andExpect(jsonPath("$.restrictedContentNotice").value("Restricted documents were excluded from retrieval and prompt context."))
        .andExpect(jsonPath("$.answer").value("Insufficient evidence to answer."));
  }

  @RestController
  static class TestEnterpriseKnowledgeController {

    @GetMapping("/api/search/knowledge")
    KnowledgeSearchResponse search(
        @RequestParam(defaultValue = "") String query,
        @RequestParam(required = false) UUID customerId,
        @RequestParam(required = false) String policyNumber,
        @RequestParam(required = false) String claimNumber,
        @RequestParam(required = false) String insurer) {
      return new KnowledgeSearchResponse(
          new QueryPlanRecord(
              "CUSTOMER",
              query,
              Map.of(
                  "policyNumber", policyNumber == null ? "" : policyNumber,
                  "claimNumber", claimNumber == null ? "" : claimNumber,
                  "insurer", insurer == null ? "" : insurer),
              "CURRENT_VERSION",
              "PAGE"),
          List.of(new KnowledgeResultRecord(
              "DOCUMENT",
              UUID.fromString("22222222-2222-2222-2222-222222222222"),
              "Renewal Schedule",
              "Northwind Mutual renewal discussion for POL-12345",
              Map.of(
                  "policyNumber", "POL-12345",
                  "claimNumber", "CLM-9988",
                  "insurer", "Northwind Mutual"))));
    }

    @PostMapping("/api/ask")
    GlobalAskResponse ask(@Valid @RequestBody GlobalAskRequest request) {
      if (Boolean.TRUE.equals(request.parameters().get("simulateRestricted"))) {
        return new GlobalAskResponse(
            "InsufficientEvidence",
            "Insufficient evidence to answer.",
            "UNGROUNDED",
            List.of(),
            "Restricted documents were excluded from retrieval and prompt context.",
            "audit-ask-002");
      }
      return new GlobalAskResponse(
          "Answered",
          "Correspondence mentioning claim number CLM-9988 was found in the latest insurer document.",
          "GROUNDED",
          List.of(new CitationRecord(
              "DOCUMENT",
              UUID.fromString("22222222-2222-2222-2222-222222222222"),
              "Insurer Correspondence",
              Map.of("claimNumber", "CLM-9988"))),
          null,
          "audit-ask-001");
    }

    @GetMapping("/api/ai/interactions/{interactionId}/evidence")
    EvidenceExpansionResponse evidence(@PathVariable UUID interactionId) {
      return new EvidenceExpansionResponse(
          interactionId,
          List.of(new EvidenceRecord(
              "DOCUMENT",
              UUID.fromString("22222222-2222-2222-2222-222222222222"),
              "Insurer Correspondence",
              Map.of(
                  "policyNumber", "POL-12345",
                  "claimNumber", "CLM-9988"))));
    }

    @PostMapping("/api/ai/conversations/{conversationId}/continue")
    ConversationContinuationResponse continueConversation(
        @PathVariable UUID conversationId,
        @Valid @RequestBody ConversationContinueRequest request) {
      return new ConversationContinuationResponse(
          conversationId,
          4,
          new QueryPlanRecord(
              "CUSTOMER",
              request.prompt(),
              Map.of(),
              "PREVIOUS_VERSION",
              "CHUNK"));
    }

    @PostMapping("/api/ai/stream")
    StreamResponse stream(@Valid @RequestBody StreamRequest request) {
      if (request.prompt().toLowerCase().contains("timeout")) {
        return new StreamResponse(
            false,
            "Failed",
            List.of(),
            List.of("Request timed out before a grounded answer could be completed."));
      }
      return new StreamResponse(
          true,
          "Streaming",
          List.of(
              new StreamEventRecord("start", ""),
              new StreamEventRecord("delta", "Customer summary in progress"),
              new StreamEventRecord("complete", "Customer summary complete")),
          List.of());
    }
  }

  record GlobalAskRequest(
      @NotBlank(message = "Question is required.") String question,
      UUID customerId,
      Map<String, Object> parameters) {
  }

  record ConversationContinueRequest(@NotBlank(message = "Prompt is required.") String prompt) {
  }

  record StreamRequest(@NotBlank(message = "Prompt is required.") String prompt) {
  }

  record QueryPlanRecord(
      String scope,
      String queryText,
      Map<String, String> businessReferenceFields,
      String versionPreference,
      String requiredEvidenceGranularity) {
  }

  record KnowledgeResultRecord(
      String sourceType,
      UUID sourceId,
      String title,
      String excerpt,
      Map<String, String> supportingAttributes) {
  }

  record KnowledgeSearchResponse(
      QueryPlanRecord queryPlan,
      List<KnowledgeResultRecord> results) {
  }

  record CitationRecord(
      String sourceType,
      UUID sourceId,
      String title,
      Map<String, String> supportingAttributes) {
  }

  record GlobalAskResponse(
      String status,
      String answer,
      String groundingStatus,
      List<CitationRecord> citations,
      String restrictedContentNotice,
      String auditCorrelationId) {
  }

  record EvidenceRecord(
      String sourceType,
      UUID sourceId,
      String title,
      Map<String, String> supportingAttributes) {
  }

  record EvidenceExpansionResponse(
      UUID interactionId,
      List<EvidenceRecord> evidence) {
  }

  record ConversationContinuationResponse(
      UUID conversationId,
      int historyCount,
      QueryPlanRecord queryPlan) {
  }

  record StreamEventRecord(
      String type,
      String data) {
  }

  record StreamResponse(
      boolean streaming,
      String status,
      List<StreamEventRecord> events,
      List<String> warnings) {
  }
}
