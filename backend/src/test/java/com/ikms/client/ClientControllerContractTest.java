package com.ikms.client;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ikms.common.api.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.time.Instant;
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
import org.springframework.web.bind.annotation.RestController;

class ClientControllerContractTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().findAndRegisterModules();
    mockMvc = MockMvcBuilders.standaloneSetup(new TestClientContractController())
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void createClientShouldRejectMissingDisplayName() throws Exception {
    var request = new ClientContracts.CreateClientRequest(
        null,
        ClientType.BUSINESS,
        "",
        "Acme Insurance LLC",
        "ops@acme.test",
        "+1-555-0100",
        "Jane Broker");

    mockMvc.perform(post("/api/clients")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("validation_error"))
        .andExpect(jsonPath("$.violations[0].field").value("displayName"));
  }

  @Test
  void getClientShouldReturnExpectedProfileShape() throws Exception {
    mockMvc.perform(get("/api/clients/{clientId}", "CL-100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.clientId").value("CL-100"))
        .andExpect(jsonPath("$.clientIdTemporary").value(false))
        .andExpect(jsonPath("$.clientType").value("BUSINESS"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.displayName").value("Acme Insurance"));
  }

  @RestController
  @RequestMapping("/api/clients")
  static class TestClientContractController {

    @PostMapping
    ClientContracts.CreateClientRequest create(@Valid @RequestBody ClientContracts.CreateClientRequest request) {
      return request;
    }

    @GetMapping("/{clientId}")
    ClientContracts.ClientProfileResponse get(@PathVariable String clientId) {
      return new ClientContracts.ClientProfileResponse(
          UUID.fromString("11111111-1111-1111-1111-111111111111"),
          clientId,
          false,
          ClientType.BUSINESS,
          ClientStatus.ACTIVE,
          "Acme Insurance",
          "Acme Insurance LLC",
          "ops@acme.test",
          "+1-555-0100",
          "Jane Broker",
          Instant.parse("2026-07-10T09:00:00Z"),
          Instant.parse("2026-07-10T09:30:00Z"));
    }
  }
}
