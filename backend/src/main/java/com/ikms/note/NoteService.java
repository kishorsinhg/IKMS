package com.ikms.note;

import com.ikms.ai.EmbeddingIndexService;
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
  private final EmbeddingIndexService embeddingIndexService;

  public NoteService(
      NoteRepository noteRepository,
      ClientService clientService,
      AuditService auditService,
      EmbeddingIndexService embeddingIndexService) {
    this.noteRepository = noteRepository;
    this.clientService = clientService;
    this.auditService = auditService;
    this.embeddingIndexService = embeddingIndexService;
  }

  @Transactional(readOnly = true)
  public List<NoteContracts.NoteResponse> list(UUID clientId) {
    return noteRepository.findByClient_IdAndStatusOrderByCreatedAtDesc(clientId, NoteStatus.ACTIVE).stream()
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
    embeddingIndexService.indexNote(clientId, saved);
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

  public NoteContracts.NoteResponse update(UUID noteId, NoteContracts.UpdateNoteRequest request, UUID actorUserId) {
    Note note = requireActiveNote(noteId);
    note.setNoteText(request.noteText().trim());
    note.setUpdatedBy(actorUserId);

    Note saved = noteRepository.save(note);
    embeddingIndexService.indexNote(saved.getClient().getId(), saved);
    auditService.write(new AuditEvent(
        Instant.now(),
        "NOTE",
        "NOTE_UPDATED",
        AuditOutcome.SUCCESS,
        actorUserId,
        saved.getClient().getId(),
        "Note",
        saved.getId().toString(),
        false,
        Map.of("clientId", saved.getClient().getId().toString())));
    return toResponse(saved);
  }

  public void softDelete(UUID noteId, UUID actorUserId) {
    Note note = requireActiveNote(noteId);
    note.setStatus(NoteStatus.DELETED);
    note.setUpdatedBy(actorUserId);
    Note saved = noteRepository.save(note);
    embeddingIndexService.deleteSource("NOTE", saved.getId());

    auditService.write(new AuditEvent(
        Instant.now(),
        "NOTE",
        "NOTE_DELETED",
        AuditOutcome.SUCCESS,
        actorUserId,
        saved.getClient().getId(),
        "Note",
        saved.getId().toString(),
        false,
        Map.of("clientId", saved.getClient().getId().toString())));
  }

  private Note requireActiveNote(UUID noteId) {
    Note note = noteRepository.findById(noteId)
        .orElseThrow(() -> new IllegalArgumentException("Note not found: " + noteId));
    if (note.getStatus() != NoteStatus.ACTIVE) {
      throw new IllegalArgumentException("Note not found: " + noteId);
    }
    return note;
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
