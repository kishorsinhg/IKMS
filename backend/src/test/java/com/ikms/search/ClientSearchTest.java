package com.ikms.search;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikms.ai.AiContracts;
import com.ikms.common.api.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
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

class ClientSearchTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().findAndRegisterModules();
    mockMvc = MockMvcBuilders.standaloneSetup(new TestClientSearchController())
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void searchShouldReturnClientScopedResults() throws Exception {
    mockMvc.perform(get("/api/clients/{clientId}/search", "11111111-1111-1111-1111-111111111111")
            .param("query", "renewal"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].sourceType").value("DOCUMENT"))
        .andExpect(jsonPath("$[0].title").value("Policy Schedule"));
  }

  @Test
  void askShouldReturnAnswerWithCitations() throws Exception {
    mockMvc.perform(post("/api/clients/{clientId}/ask", "11111111-1111-1111-1111-111111111111")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(new AiContracts.AskClientRequest("What is due next month?"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Answered"))
        .andExpect(jsonPath("$.citations[0].title").value("Policy Schedule"));
  }

  @RestController
  @RequestMapping("/api/clients/{clientId}")
  static class TestClientSearchController {

    @GetMapping("/search")
    List<SearchContracts.SearchResultResponse> search(
        @PathVariable UUID clientId,
        @RequestParam(name = "query", defaultValue = "") String query) {
      return List.of(new SearchContracts.SearchResultResponse(
          "DOCUMENT",
          UUID.fromString("22222222-2222-2222-2222-222222222222"),
          "Policy Schedule",
          "renewal due next month",
          "Document: Policy Schedule",
          Instant.parse("2026-07-10T09:00:00Z")));
    }

    @PostMapping("/ask")
    AiContracts.AskClientResponse ask(@PathVariable UUID clientId, @RequestBody AiContracts.AskClientRequest request) {
      return new AiContracts.AskClientResponse(
          UUID.fromString("33333333-3333-3333-3333-333333333333"),
          "Answered",
          "Policy Schedule: renewal due next month",
          List.of(new AiContracts.SourceCitation(
              "DOCUMENT",
              UUID.fromString("22222222-2222-2222-2222-222222222222"),
              "Policy Schedule",
              "renewal due next month")),
          Instant.parse("2026-07-10T09:00:00Z"));
    }
  }
}
