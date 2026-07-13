package com.ikms.note;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ikms.common.api.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class NoteControllerContractTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().findAndRegisterModules();
    mockMvc = MockMvcBuilders.standaloneSetup(new TestNoteContractController())
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void createNoteShouldRejectBlankText() throws Exception {
    var request = new NoteContracts.CreateNoteRequest("");

    mockMvc.perform(post("/api/clients/{clientId}/notes", "11111111-1111-1111-1111-111111111111")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("validation_error"))
        .andExpect(jsonPath("$.violations[0].field").value("noteText"));
  }

  @Test
  void listNotesShouldReturnClientScopedShape() throws Exception {
    mockMvc.perform(get("/api/clients/{clientId}/notes", "11111111-1111-1111-1111-111111111111"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].clientId").value("11111111-1111-1111-1111-111111111111"))
        .andExpect(jsonPath("$[0].status").value("ACTIVE"))
        .andExpect(jsonPath("$[0].noteText").value("Policy renewal follow-up required."));
  }

  @Test
  void updateNoteShouldRejectBlankText() throws Exception {
    var request = new NoteContracts.UpdateNoteRequest("");

    mockMvc.perform(patch("/api/notes/{noteId}", "22222222-2222-2222-2222-222222222222")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("validation_error"))
        .andExpect(jsonPath("$.violations[0].field").value("noteText"));
  }

  @Test
  void deleteNoteShouldReturnOk() throws Exception {
    mockMvc.perform(delete("/api/notes/{noteId}", "22222222-2222-2222-2222-222222222222"))
        .andExpect(status().isOk());
  }

  @RestController
  @RequestMapping("/api")
  static class TestNoteContractController {

    @PostMapping("/clients/{clientId}/notes")
    NoteContracts.CreateNoteRequest create(@Valid @RequestBody NoteContracts.CreateNoteRequest request) {
      return request;
    }

    @GetMapping("/clients/{clientId}/notes")
    List<NoteContracts.NoteResponse> list(@PathVariable UUID clientId) {
      return List.of(new NoteContracts.NoteResponse(
          UUID.fromString("22222222-2222-2222-2222-222222222222"),
          clientId,
          "Policy renewal follow-up required.",
          NoteStatus.ACTIVE,
          Instant.parse("2026-07-10T10:00:00Z"),
          Instant.parse("2026-07-10T10:15:00Z")));
    }

    @PatchMapping("/notes/{noteId}")
    NoteContracts.UpdateNoteRequest update(@Valid @RequestBody NoteContracts.UpdateNoteRequest request) {
      return request;
    }

    @DeleteMapping("/notes/{noteId}")
    void delete(@PathVariable UUID noteId) {
    }
  }
}
