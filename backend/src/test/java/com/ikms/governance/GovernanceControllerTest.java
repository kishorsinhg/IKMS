package com.ikms.governance;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikms.common.api.GlobalExceptionHandler;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class GovernanceControllerTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().findAndRegisterModules();
    mockMvc = MockMvcBuilders.standaloneSetup(new TestGovernanceController())
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void classificationPolicyShouldReturnExpectedShape() throws Exception {
    mockMvc.perform(get("/api/governance/classification"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.defaultClassification").value("INTERNAL"))
        .andExpect(jsonPath("$.levels[3]").value("RESTRICTED"));
  }

  @Test
  void aiGovernanceShouldValidateApprovedModels() throws Exception {
    mockMvc.perform(post("/api/governance/ai")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(new GovernanceContracts.AiGovernancePolicyRequest(
                List.of("openai:gpt-5-mini"),
                "prompt-policy-v2",
                "response-policy-v2",
                true,
                true))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.approvedModels[0]").value("openai:gpt-5-mini"))
        .andExpect(jsonPath("$.promptPolicyVersion").value("prompt-policy-v2"));
  }

  @RestController
  @RequestMapping("/api/governance")
  static class TestGovernanceController {

    @GetMapping("/classification")
    GovernanceContracts.ClassificationPolicyResponse classification() {
      return new GovernanceContracts.ClassificationPolicyResponse(
          List.of("PUBLIC", "INTERNAL", "CONFIDENTIAL", "RESTRICTED", "HIGHLY_RESTRICTED"),
          "INTERNAL",
          "RESTRICTED",
          "CONFIDENTIAL",
          Instant.parse("2026-07-15T12:00:00Z"));
    }

    @PostMapping("/ai")
    GovernanceContracts.AiGovernancePolicyResponse ai(@Valid @RequestBody GovernanceContracts.AiGovernancePolicyRequest request) {
      return new GovernanceContracts.AiGovernancePolicyResponse(
          request.approvedModels(),
          request.promptPolicyVersion(),
          request.responsePolicyVersion(),
          request.citationRequired(),
          request.groundingValidationRequired(),
          Instant.parse("2026-07-15T12:00:00Z"));
    }
  }
}
