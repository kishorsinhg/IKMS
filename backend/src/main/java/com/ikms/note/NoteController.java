package com.ikms.note;

import com.ikms.security.AppUserPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

  private final NoteService noteService;

  public NoteController(NoteService noteService) {
    this.noteService = noteService;
  }

  @PatchMapping("/{noteId}")
  public NoteContracts.NoteResponse update(
      @PathVariable UUID noteId,
      @Valid @RequestBody NoteContracts.UpdateNoteRequest request,
      Authentication authentication) {
    return noteService.update(noteId, request, principal(authentication).id());
  }

  @DeleteMapping("/{noteId}")
  public void delete(@PathVariable UUID noteId, Authentication authentication) {
    noteService.softDelete(noteId, principal(authentication).id());
  }

  private AppUserPrincipal principal(Authentication authentication) {
    return (AppUserPrincipal) authentication.getPrincipal();
  }
}
