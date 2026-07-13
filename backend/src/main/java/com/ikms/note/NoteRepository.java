package com.ikms.note;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<Note, UUID> {

  List<Note> findByClient_IdOrderByCreatedAtDesc(UUID clientId);

  List<Note> findByClient_IdAndStatusOrderByCreatedAtDesc(UUID clientId, NoteStatus status);
}
