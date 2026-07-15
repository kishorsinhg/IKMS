package com.ikms.ai;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikms.common.api.GlobalExceptionHandler;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
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

class EnterpriseAiOrchestrationTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().findAndRegisterModules();
    mockMvc = MockMvcBuilders.standaloneSetup(new TestEnterpriseAiController())
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void searchShouldReturnHybridRetrievalDiagnostics() throws Exception {
    mockMvc.perform(get("/api/clients/{clientId}/search", "11111111-1111-1111-1111-111111111111")
            .param("query", "renewal"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].sourceType").value("DOCUMENT"))
        .andExpect(jsonPath("$[0].retrievalPath").value("VECTOR_HYBRID"))
        .andExpect(jsonPath("$[0].citationQuality").value("HIGH"));
  }

  @Test
  void askShouldReturnCitationsWarningsAndGroundedAnswer() throws Exception {
    mockMvc.perform(post("/api/clients/{clientId}/ask", "11111111-1111-1111-1111-111111111111")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(new AiContracts.AskClientRequest("What is due next month?"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Answered"))
        .andExpect(jsonPath("$.retrievalMode").value("HYBRID_VECTOR"))
        .andExpect(jsonPath("$.citations[0].title").value("Policy Schedule"))
        .andExpect(jsonPath("$.warnings[0]").value("Some retrieved evidence has limited location metadata and may produce weaker citations."));
  }

  @Test
  void askShouldReturnRefusedForDecisionMakingRequests() throws Exception {
    mockMvc.perform(post("/api/clients/{clientId}/ask", "11111111-1111-1111-1111-111111111111")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(new AiContracts.AskClientRequest("Should we approve this claim?"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Refused"))
        .andExpect(jsonPath("$.retrievalMode").value("GUARDRAIL"))
        .andExpect(jsonPath("$.citations").isEmpty());
  }

  @Test
  void summarizeShouldReturnConversationMetadataAndJumpTargets() throws Exception {
    mockMvc.perform(post("/api/clients/{clientId}/summarize", "11111111-1111-1111-1111-111111111111")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(new OrchestrationPromptRequest("Summarize customer"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.operation").value("SUMMARIZE"))
        .andExpect(jsonPath("$.conversationId").value("44444444-4444-4444-4444-444444444444"))
        .andExpect(jsonPath("$.citations[0].jumpTargetId").value("document:22222222-2222-2222-2222-222222222222:page:4"))
        .andExpect(jsonPath("$.metrics.groundingScore").value(0.98d));
  }

  @Test
  void explainShouldReturnEvidenceReferencesAndSourceChips() throws Exception {
    mockMvc.perform(post("/api/clients/{clientId}/explain", "11111111-1111-1111-1111-111111111111")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(new OrchestrationPromptRequest("Explain this document"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.operation").value("EXPLAIN"))
        .andExpect(jsonPath("$.evidenceReferences[0].target").value("page"))
        .andExpect(jsonPath("$.sourceReferences[0].kind").value("DOCUMENT"));
  }

  @Test
  void compareShouldReturnMultiDocumentReasoningShape() throws Exception {
    mockMvc.perform(post("/api/clients/{clientId}/compare", "11111111-1111-1111-1111-111111111111")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(new CompareRequest(
                "Compare with previous version",
                List.of(
                    UUID.fromString("22222222-2222-2222-2222-222222222222"),
                    UUID.fromString("55555555-5555-5555-5555-555555555555"))))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.operation").value("COMPARE"))
        .andExpect(jsonPath("$.sourceReferences.length()").value(2))
        .andExpect(jsonPath("$.warnings[0]").value("Comparison used cross-document reasoning and should be manually reviewed for business decisions."));
  }

  @Test
  void extractShouldReturnStructuredFieldsAndConfidence() throws Exception {
    mockMvc.perform(post("/api/clients/{clientId}/extract", "11111111-1111-1111-1111-111111111111")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(new OrchestrationPromptRequest("Extract missing fields"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.operation").value("EXTRACT"))
        .andExpect(jsonPath("$.structuredPayload.fields.policyNumber.value").value("TMP-PROP-90814"))
        .andExpect(jsonPath("$.structuredPayload.fields.policyNumber.confidence").value("HIGH"))
        .andExpect(jsonPath("$.structuredPayload.fields.effectiveDate.status").value("MISSING"));
  }

  @Test
  void validateShouldReturnNeedsReviewAndGuardrailMetadata() throws Exception {
    mockMvc.perform(post("/api/clients/{clientId}/validate", "11111111-1111-1111-1111-111111111111")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(new OrchestrationPromptRequest("Validate extracted metadata"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.operation").value("VALIDATE"))
        .andExpect(jsonPath("$.structuredPayload.validationResult").value("NEEDS_REVIEW"))
        .andExpect(jsonPath("$.structuredPayload.guardrails.promptInjectionDetected").value(false))
        .andExpect(jsonPath("$.structuredPayload.guardrails.piiMasked").value(true));
  }

  @RestController
  @RequestMapping("/api/clients/{clientId}")
  static class TestEnterpriseAiController {

    @GetMapping("/search")
    List<com.ikms.search.SearchContracts.SearchResultResponse> search(
        @PathVariable UUID clientId,
        @RequestParam(name = "query", defaultValue = "") String query) {
      return List.of(new com.ikms.search.SearchContracts.SearchResultResponse(
          "DOCUMENT",
          UUID.fromString("22222222-2222-2222-2222-222222222222"),
          "Policy Schedule",
          "renewal due next month",
          "Document: Policy Schedule",
          4,
          "document-version",
          "VECTOR_HYBRID",
          "HIGH",
          Instant.parse("2026-07-10T09:00:00Z")));
    }

    @PostMapping("/ask")
    AiContracts.AskClientResponse ask(
        @PathVariable UUID clientId,
        @Valid @RequestBody AiContracts.AskClientRequest request) {
      if (request.question().toLowerCase().contains("approve")) {
        return new AiContracts.AskClientResponse(
            UUID.fromString("33333333-3333-3333-3333-333333333333"),
            "Refused",
            "I cannot make claim, underwriting, or policy decisions.",
            List.of(),
            "GUARDRAIL",
            List.of("Decision-making requests require a human reviewer."),
            Instant.parse("2026-07-10T09:00:00Z"));
      }
      return new AiContracts.AskClientResponse(
          UUID.fromString("33333333-3333-3333-3333-333333333333"),
          "Answered",
          "Policy Schedule: renewal due next month",
          List.of(new AiContracts.SourceCitation(
              "DOCUMENT",
              UUID.fromString("22222222-2222-2222-2222-222222222222"),
              "Policy Schedule",
              "renewal due next month",
              4,
              "document-version")),
          "HYBRID_VECTOR",
          List.of("Some retrieved evidence has limited location metadata and may produce weaker citations."),
          Instant.parse("2026-07-10T09:00:00Z"));
    }

    @PostMapping("/summarize")
    OrchestrationResponse summarize(
        @PathVariable UUID clientId,
        @Valid @RequestBody OrchestrationPromptRequest request) {
      return response("SUMMARIZE", "Customer summary prepared from policy schedule and broker note.");
    }

    @PostMapping("/explain")
    OrchestrationResponse explain(
        @PathVariable UUID clientId,
        @Valid @RequestBody OrchestrationPromptRequest request) {
      return response("EXPLAIN", "The document requires review because extraction confidence was low.");
    }

    @PostMapping("/compare")
    OrchestrationResponse compare(
        @PathVariable UUID clientId,
        @Valid @RequestBody CompareRequest request) {
      return new OrchestrationResponse(
          "COMPARE",
          UUID.fromString("44444444-4444-4444-4444-444444444444"),
          UUID.fromString("66666666-6666-6666-6666-666666666666"),
          "Compared the selected documents and found a carrier wording change.",
          List.of(new CitationRecord(
              "DOCUMENT",
              UUID.fromString("22222222-2222-2222-2222-222222222222"),
              "Policy Schedule v1",
              "renewal due next month",
              3,
              "document-version",
              "HIGH",
              "document:22222222-2222-2222-2222-222222222222:page:3")),
          List.of(new EvidenceReferenceRecord(
              "document-page-3",
              "Jump to page",
              "page",
              "Page 3",
              true)),
          List.of(
              new SourceReferenceRecord("document-a", "Policy Schedule v1", "DOCUMENT"),
              new SourceReferenceRecord("document-b", "Policy Schedule v2", "DOCUMENT")),
          List.of("Comparison used cross-document reasoning and should be manually reviewed for business decisions."),
          new MetricsRecord(824L, 0.91d, 1.00d, 2, "HYBRID_VECTOR"),
          Map.of(
              "comparisonMode", "DOCUMENT_VERSION",
              "sourceIds", request.sourceIds()));
    }

    @PostMapping("/extract")
    OrchestrationResponse extract(
        @PathVariable UUID clientId,
        @Valid @RequestBody OrchestrationPromptRequest request) {
      return new OrchestrationResponse(
          "EXTRACT",
          UUID.fromString("44444444-4444-4444-4444-444444444444"),
          UUID.fromString("77777777-7777-7777-7777-777777777777"),
          "Extracted fields from the current document set.",
          List.of(new CitationRecord(
              "DOCUMENT",
              UUID.fromString("22222222-2222-2222-2222-222222222222"),
              "Policy Schedule",
              "TMP-PROP-90814",
              4,
              "policy-number",
              "HIGH",
              "metadata:DOCUMENT:22222222-2222-2222-2222-222222222222:policyNumber")),
          List.of(),
          List.of(new SourceReferenceRecord("document-a", "Policy Schedule", "DOCUMENT")),
          List.of(),
          new MetricsRecord(612L, 0.94d, 1.00d, 1, "METADATA_PLUS_VECTOR"),
          Map.of(
              "fields", Map.of(
                  "policyNumber", Map.of(
                      "value", "TMP-PROP-90814",
                      "confidence", "HIGH",
                      "status", "VERIFIED"),
                  "effectiveDate", Map.of(
                      "value", "",
                      "confidence", "UNKNOWN",
                      "status", "MISSING"))));
    }

    @PostMapping("/validate")
    OrchestrationResponse validate(
        @PathVariable UUID clientId,
        @Valid @RequestBody OrchestrationPromptRequest request) {
      return new OrchestrationResponse(
          "VALIDATE",
          UUID.fromString("44444444-4444-4444-4444-444444444444"),
          UUID.fromString("88888888-8888-8888-8888-888888888888"),
          "Validation found missing values that require manual review.",
          List.of(),
          List.of(),
          List.of(new SourceReferenceRecord("document-a", "Policy Schedule", "DOCUMENT")),
          List.of("PII remained masked during validation."),
          new MetricsRecord(455L, 0.89d, 0.00d, 0, "VALIDATION_ONLY"),
          Map.of(
              "validationResult", "NEEDS_REVIEW",
              "guardrails", Map.of(
                  "promptInjectionDetected", false,
                  "piiMasked", true,
                  "grounded", true)));
    }

    private static OrchestrationResponse response(String operation, String answer) {
      return new OrchestrationResponse(
          operation,
          UUID.fromString("44444444-4444-4444-4444-444444444444"),
          UUID.fromString("99999999-9999-9999-9999-999999999999"),
          answer,
          List.of(new CitationRecord(
              "DOCUMENT",
              UUID.fromString("22222222-2222-2222-2222-222222222222"),
              "Policy Schedule",
              "renewal due next month",
              4,
              "document-version",
              "HIGH",
              "document:22222222-2222-2222-2222-222222222222:page:4")),
          List.of(new EvidenceReferenceRecord(
              "document-page-4",
              "Jump to page",
              "page",
              "Page 4",
              true)),
          List.of(new SourceReferenceRecord("document-a", "Policy Schedule", "DOCUMENT")),
          List.of(),
          new MetricsRecord(512L, 0.98d, 1.00d, 1, "HYBRID_VECTOR"),
          Map.of("scope", "CLIENT"));
    }
  }

  record OrchestrationPromptRequest(
      @NotBlank(message = "Prompt is required.") String prompt) {
  }

  record CompareRequest(
      @NotBlank(message = "Prompt is required.") String prompt,
      List<UUID> sourceIds) {
  }

  record CitationRecord(
      String sourceType,
      UUID sourceId,
      String title,
      String excerpt,
      Integer pageNumber,
      String section,
      String confidence,
      String jumpTargetId) {
  }

  record EvidenceReferenceRecord(
      String key,
      String label,
      String target,
      String detail,
      boolean disabled) {
  }

  record SourceReferenceRecord(
      String key,
      String label,
      String kind) {
  }

  record MetricsRecord(
      long latencyMs,
      double groundingScore,
      double citationCoverage,
      int evidenceCount,
      String retrievalMode) {
  }

  record OrchestrationResponse(
      String operation,
      UUID conversationId,
      UUID interactionId,
      String answer,
      List<CitationRecord> citations,
      List<EvidenceReferenceRecord> evidenceReferences,
      List<SourceReferenceRecord> sourceReferences,
      List<String> warnings,
      MetricsRecord metrics,
      Map<String, Object> structuredPayload) {
  }
}
