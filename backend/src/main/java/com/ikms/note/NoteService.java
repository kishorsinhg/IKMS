package com.ikms.note;

import com.ikms.audit.AuditService;
import com.ikms.audit.AuditService.AuditEvent;
import com.ikms.audit.AuditService.AuditOutcome;
import com.ikms.client.ClientService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NoteService {

  private final NoteRepository noteRepository;
  private final ClientService clientService;
  private final AuditService auditService;

  public NoteService(NoteRepository noteRepository, ClientService clientService, AuditService auditService) {
    this.noteRepository = noteRepository;
    this.clientService = clientService;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public List<NoteContracts.NoteResponse> list(UUID clientId) {
    return noteRepository.findByClient_IdOrderByCreatedAtDesc(clientId).stream()
        .map(this::toResponse)
        .toList();
  }

  public NoteContracts.NoteResponse create(UUID clientId, NoteContracts.CreateNoteRequest request, UUID actorUserId) {
    Note note = new Note();
    note.setClient(clientService.requireClient(clientId));
    note.setNoteText(request.noteText().trim());
    note.setStatus(NoteStatus.ACTIVE);
    note.setCreatedBy(actorUserId);
    note.setUpdatedBy(actorUserId);

    Note saved = noteRepository.save(note);
    auditService.write(new AuditEvent(
        Instant.now(),
        "NOTE",
        "NOTE_CREATED",
        AuditOutcome.SUCCESS,
        actorUserId,
        clientId,
        "Note",
        saved.getId().toString(),
        false,
        Map.of("clientId", clientId.toString())));
    return toResponse(saved);
  }

  private NoteContracts.NoteResponse toResponse(Note note) {
    return new NoteContracts.NoteResponse(
        note.getId(),
        note.getClient().getId(),
        note.getNoteText(),
        note.getStatus(),
        note.getCreatedAt(),
        note.getUpdatedAt());
  }
}
