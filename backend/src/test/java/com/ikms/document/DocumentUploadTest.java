package com.ikms.document;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikms.common.api.GlobalExceptionHandler;
import jakarta.validation.Valid;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class DocumentUploadTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().findAndRegisterModules();
    mockMvc = MockMvcBuilders.standaloneSetup(new TestDocumentUploadController())
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void uploadShouldRejectMissingFilename() throws Exception {
    var request = new DocumentContracts.UploadDocumentRequest(
        null,
        "",
        "application/pdf",
        "abc123");

    mockMvc.perform(post("/api/documents/upload")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.violations[0].field").value("filename"));
  }

  @Test
  void uploadShouldReturnDuplicateOutcomeShape() throws Exception {
    var request = new DocumentContracts.UploadDocumentRequest(
        UUID.fromString("11111111-1111-1111-1111-111111111111"),
        "renewal.pdf",
        "application/pdf",
        "dup-hash-001");

    mockMvc.perform(post("/api/documents/upload")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.outcome").value("DUPLICATE"))
        .andExpect(jsonPath("$.reviewStatus").value("PENDING_REVIEW"))
        .andExpect(jsonPath("$.duplicateOfDocumentId").value("44444444-4444-4444-4444-444444444444"));
  }

  @RestController
  @RequestMapping("/api/documents")
  static class TestDocumentUploadController {

    @PostMapping("/upload")
    DocumentContracts.UploadDocumentResponse upload(@Valid @RequestBody DocumentContracts.UploadDocumentRequest request) {
      return new DocumentContracts.UploadDocumentResponse(
          UUID.fromString("33333333-3333-3333-3333-333333333333"),
          UUID.fromString("55555555-5555-5555-5555-555555555555"),
          "DUPLICATE",
          "PENDING_REVIEW",
          "44444444-4444-4444-4444-444444444444");
    }
  }
}
