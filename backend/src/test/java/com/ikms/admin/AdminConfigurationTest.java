package com.ikms.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikms.common.api.GlobalExceptionHandler;
import com.ikms.config.AdminConfigurationContracts;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class AdminConfigurationTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().findAndRegisterModules();
    mockMvc = MockMvcBuilders.standaloneSetup(new TestAdminConfigurationController())
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void documentTypesShouldReturnConfigurationShape() throws Exception {
    mockMvc.perform(get("/api/admin/document-types"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Policy Schedule"))
        .andExpect(jsonPath("$[0].active").value(true));
  }

  @Test
  void metadataFieldCreateShouldValidateFieldKey() throws Exception {
    mockMvc.perform(post("/api/admin/metadata-fields")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(new AdminConfigurationContracts.MetadataFieldRequest("", "Carrier", true, true))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.violations[0].field").value("fieldKey"));
  }

  @Test
  void reviewSettingPatchShouldReturnUpdatedValues() throws Exception {
    mockMvc.perform(patch("/api/admin/review-settings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(new AdminConfigurationContracts.ReviewSettingRequest("manual", 0.82d))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mode").value("manual"))
        .andExpect(jsonPath("$.lowConfidenceThreshold").value(0.82d));
  }

  @RestController
  @RequestMapping("/api/admin")
  static class TestAdminConfigurationController {

    @GetMapping("/document-types")
    List<AdminConfigurationContracts.DocumentTypeResponse> listDocumentTypes() {
      return List.of(new AdminConfigurationContracts.DocumentTypeResponse(
          UUID.fromString("11111111-1111-1111-1111-111111111111"),
          "Policy Schedule",
          "Policy documents",
          true,
          Instant.parse("2026-07-10T09:00:00Z")));
    }

    @PostMapping("/metadata-fields")
    AdminConfigurationContracts.MetadataFieldRequest createMetadataField(@Valid @RequestBody AdminConfigurationContracts.MetadataFieldRequest request) {
      return request;
    }

    @PatchMapping("/review-settings")
    AdminConfigurationContracts.ReviewSettingResponse updateReviewSetting(@Valid @RequestBody AdminConfigurationContracts.ReviewSettingRequest request) {
      return new AdminConfigurationContracts.ReviewSettingResponse(
          UUID.fromString("22222222-2222-2222-2222-222222222222"),
          request.mode(),
          request.lowConfidenceThreshold(),
          Instant.parse("2026-07-10T09:00:00Z"));
    }
  }
}
