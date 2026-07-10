package com.ikms.client;

import com.ikms.note.NoteContracts;
import com.ikms.note.NoteService;
import com.ikms.security.AppUserPrincipal;
import com.ikms.security.PiiMaskingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

  private final ClientService clientService;
  private final NoteService noteService;
  private final PiiMaskingService piiMaskingService;

  public ClientController(ClientService clientService, NoteService noteService, PiiMaskingService piiMaskingService) {
    this.clientService = clientService;
    this.noteService = noteService;
    this.piiMaskingService = piiMaskingService;
  }

  @GetMapping
  public List<ClientContracts.ClientSummaryResponse> list(
      @RequestParam(name = "query", defaultValue = "") String query) {
    return clientService.search(query);
  }

  @PostMapping
  public ClientContracts.ClientProfileResponse create(
      @Valid @RequestBody ClientContracts.CreateClientRequest request,
      Authentication authentication) {
    return clientService.create(request, principal(authentication).id());
  }

  @GetMapping("/{clientId}")
  public ClientContracts.ClientProfileResponse get(@PathVariable UUID clientId, Authentication authentication) {
    return piiMaskingService.maskClientProfile(
        clientService.get(clientId),
        principal(authentication).permissions());
  }

  @PatchMapping("/{clientId}")
  public ClientContracts.ClientProfileResponse update(
      @PathVariable UUID clientId,
      @Valid @RequestBody ClientContracts.UpdateClientRequest request,
      Authentication authentication) {
    return clientService.update(clientId, request, principal(authentication).id());
  }

  @GetMapping("/{clientId}/notes")
  public List<NoteContracts.NoteResponse> listNotes(@PathVariable UUID clientId) {
    return noteService.list(clientId);
  }

  @PostMapping("/{clientId}/notes")
  public NoteContracts.NoteResponse createNote(
      @PathVariable UUID clientId,
      @Valid @RequestBody NoteContracts.CreateNoteRequest request,
      Authentication authentication) {
    return noteService.create(clientId, request, principal(authentication).id());
  }

  private AppUserPrincipal principal(Authentication authentication) {
    return (AppUserPrincipal) authentication.getPrincipal();
  }
}
