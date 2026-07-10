package com.ikms.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class NoteContracts {

  private NoteContracts() {
  }

  public record CreateNoteRequest(
      @NotBlank(message = "Note text is required.")
      @Size(max = 4000, message = "Note text must be 4000 characters or fewer.")
      String noteText) {
  }

  public record NoteResponse(
      UUID id,
      UUID clientId,
      String noteText,
      NoteStatus status,
      Instant createdAt,
      Instant updatedAt) {
  }
}
