package com.ikms.client;

import com.ikms.note.NoteContracts;
import com.ikms.note.NoteService;
import com.ikms.observability.RequestContextHolder;
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
  private final ClientKnowledgeService clientKnowledgeService;
  private final NoteService noteService;
  private final PiiMaskingService piiMaskingService;

  public ClientController(
      ClientService clientService,
      ClientKnowledgeService clientKnowledgeService,
      NoteService noteService,
      PiiMaskingService piiMaskingService) {
    this.clientService = clientService;
    this.clientKnowledgeService = clientKnowledgeService;
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

  @GetMapping("/{clientId}/knowledge/timeline")
  public ClientContracts.CustomerKnowledgeTimelinePageResponse timeline(
      @PathVariable UUID clientId,
      @RequestParam(name = "cursor", required = false) String cursor,
      @RequestParam(name = "limit", required = false) Integer limit,
      @RequestParam(name = "query", required = false) String query,
      @RequestParam(name = "from", required = false) String from,
      @RequestParam(name = "to", required = false) String to,
      @RequestParam(name = "sourceType", required = false) String sourceType,
      @RequestParam(name = "eventType", required = false) String eventType,
      @RequestParam(name = "documentType", required = false) String documentType,
      @RequestParam(name = "reviewStatus", required = false) String reviewStatus,
      @RequestParam(name = "policyNumber", required = false) String policyNumber,
      @RequestParam(name = "claimNumber", required = false) String claimNumber,
      @RequestParam(name = "insurer", required = false) String insurer,
      @RequestParam(name = "actor", required = false) String actor,
      Authentication authentication) {
    try (RequestContextHolder.Scope ignored = RequestContextHolder.withGenerated(RequestContextHolder.TIMELINE_REQUEST_ID)) {
      AppUserPrincipal principal = principal(authentication);
      return clientKnowledgeService.timeline(
          clientId,
          new ClientKnowledgeService.TimelineQuery(
              cursor,
              limit,
              query,
              from,
              to,
              sourceType,
              eventType,
              documentType,
              reviewStatus,
              policyNumber,
              claimNumber,
              insurer,
              actor),
          principal.id(),
          principal.permissions());
    }
  }

  @GetMapping("/{clientId}/knowledge/related")
  public ClientContracts.RelatedKnowledgeResponse relatedKnowledge(
      @PathVariable UUID clientId,
      @RequestParam(name = "limit", required = false) Integer limit,
      Authentication authentication) {
    try (RequestContextHolder.Scope ignored = RequestContextHolder.withGenerated(RequestContextHolder.TIMELINE_REQUEST_ID)) {
      AppUserPrincipal principal = principal(authentication);
      return clientKnowledgeService.relatedForClient(clientId, limit == null ? 20 : limit, principal.id(), principal.permissions());
    }
  }

  private AppUserPrincipal principal(Authentication authentication) {
    return (AppUserPrincipal) authentication.getPrincipal();
  }
}
