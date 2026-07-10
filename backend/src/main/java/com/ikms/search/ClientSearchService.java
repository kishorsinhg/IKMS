package com.ikms.search;

import com.ikms.document.Document;
import com.ikms.document.DocumentRepository;
import com.ikms.document.DocumentVersion;
import com.ikms.document.DocumentVersionRepository;
import com.ikms.email.Email;
import com.ikms.email.EmailRepository;
import com.ikms.note.Note;
import com.ikms.note.NoteRepository;
import com.ikms.note.NoteStatus;
import com.ikms.security.SecurityTrimService;
import com.ikms.security.domain.Permission;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClientSearchService {

  private final DocumentRepository documentRepository;
  private final DocumentVersionRepository documentVersionRepository;
  private final EmailRepository emailRepository;
  private final NoteRepository noteRepository;
  private final SecurityTrimService securityTrimService;

  public ClientSearchService(
      DocumentRepository documentRepository,
      DocumentVersionRepository documentVersionRepository,
      EmailRepository emailRepository,
      NoteRepository noteRepository,
      SecurityTrimService securityTrimService) {
    this.documentRepository = documentRepository;
    this.documentVersionRepository = documentVersionRepository;
    this.emailRepository = emailRepository;
    this.noteRepository = noteRepository;
    this.securityTrimService = securityTrimService;
  }

  public List<SearchContracts.SearchResultResponse> search(UUID clientId, String query, Set<Permission> permissions) {
    String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    List<SearchContracts.SearchResultResponse> results = new ArrayList<>();

    for (Document document : documentRepository.findByClient_IdOrderByCreatedAtDesc(clientId)) {
      DocumentVersion version = documentVersionRepository.findByDocument_IdAndCurrentTrue(document.getId()).orElse(null);
      String haystack = (document.getTitle() + " " + (version == null ? "" : nullSafe(version.getExtractedText())))
          .toLowerCase(Locale.ROOT);
      if (normalizedQuery.isBlank() || haystack.contains(normalizedQuery)) {
        String excerpt = version == null ? document.getTitle() : excerpt(version.getExtractedText(), document.getTitle(), normalizedQuery);
        boolean containsPii = document.getClient() != null;
        results.add(new SearchContracts.SearchResultResponse(
            "DOCUMENT",
            document.getId(),
            document.getTitle(),
            securityTrimService.trimSearchResult(permissions, excerpt, containsPii),
            "Document: " + document.getTitle(),
            document.getCreatedAt()));
      }
    }

    for (Email email : emailRepository.findByClient_IdOrderByReceivedAtDesc(clientId)) {
      String haystack = (email.getSubject() + " " + nullSafe(email.getBodyText())).toLowerCase(Locale.ROOT);
      if (normalizedQuery.isBlank() || haystack.contains(normalizedQuery)) {
        String excerpt = excerpt(email.getBodyText(), email.getSubject(), normalizedQuery);
        results.add(new SearchContracts.SearchResultResponse(
            "EMAIL",
            email.getId(),
            email.getSubject(),
            securityTrimService.trimSearchResult(permissions, excerpt, true),
            "Email: " + email.getSubject(),
            email.getReceivedAt()));
      }
    }

    for (Note note : noteRepository.findByClient_IdOrderByCreatedAtDesc(clientId)) {
      if (note.getStatus() != NoteStatus.ACTIVE) {
        continue;
      }
      String haystack = note.getNoteText().toLowerCase(Locale.ROOT);
      if (normalizedQuery.isBlank() || haystack.contains(normalizedQuery)) {
        results.add(new SearchContracts.SearchResultResponse(
            "NOTE",
            note.getId(),
            "Broker note",
            securityTrimService.trimSearchResult(permissions, excerpt(note.getNoteText(), "Broker note", normalizedQuery), true),
            "Note created " + note.getCreatedAt(),
            note.getCreatedAt()));
      }
    }

    return results.stream()
        .sorted(Comparator.comparing(SearchContracts.SearchResultResponse::occurredAt, Comparator.nullsLast(Comparator.reverseOrder())))
        .limit(20)
        .toList();
  }

  private static String excerpt(String text, String fallback, String query) {
    String value = nullSafe(text).trim();
    if (value.isBlank()) {
      return fallback;
    }
    if (query == null || query.isBlank()) {
      return truncate(value);
    }
    int index = value.toLowerCase(Locale.ROOT).indexOf(query);
    if (index < 0) {
      return truncate(value);
    }
    int start = Math.max(0, index - 40);
    int end = Math.min(value.length(), index + query.length() + 80);
    return value.substring(start, end).trim();
  }

  private static String truncate(String value) {
    return value.length() <= 160 ? value : value.substring(0, 160).trim() + "...";
  }

  private static String nullSafe(String value) {
    return value == null ? "" : value;
  }
}
