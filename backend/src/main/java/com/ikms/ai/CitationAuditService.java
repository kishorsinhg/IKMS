package com.ikms.ai;

import com.ikms.ai.orchestration.EnterpriseAiContracts;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CitationAuditService {

  private final AiCitationRecordRepository aiCitationRecordRepository;
  private final AiRetrievalTraceRepository aiRetrievalTraceRepository;

  public CitationAuditService(
      AiCitationRecordRepository aiCitationRecordRepository,
      AiRetrievalTraceRepository aiRetrievalTraceRepository) {
    this.aiCitationRecordRepository = aiCitationRecordRepository;
    this.aiRetrievalTraceRepository = aiRetrievalTraceRepository;
  }

  public void record(
      UUID interactionId,
      UUID conversationId,
      UUID clientId,
      String retrievalMode,
      List<EnterpriseAiContracts.CitationReference> citations,
      List<EnterpriseAiContracts.RetrievedEvidence> evidence,
      EnterpriseGuardrailService.GuardrailOutcome guardrailOutcome) {
    for (EnterpriseAiContracts.CitationReference citation : citations) {
      AiCitationRecord record = new AiCitationRecord();
      record.setInteractionId(interactionId);
      record.setConversationId(conversationId);
      record.setSourceType(citation.sourceType());
      record.setSourceId(citation.sourceId());
      record.setTitle(citation.title());
      record.setExcerpt(citation.excerpt());
      record.setPageNumber(citation.pageNumber());
      record.setChunkIndex(citation.chunkIndex());
      record.setSourceSection(citation.section());
      record.setConfidence(citation.confidence());
      record.setEvidenceText(citation.excerpt());
      record.setJumpTargetId(citation.jumpTargetId());
      record.setRetrievalPath(citation.retrievalPath());
      aiCitationRecordRepository.save(record);
    }

    for (EnterpriseAiContracts.RetrievedEvidence item : evidence) {
      AiRetrievalTrace trace = new AiRetrievalTrace();
      trace.setInteractionId(interactionId);
      trace.setConversationId(conversationId);
      trace.setClientId(clientId);
      trace.setRetrievalMode(retrievalMode);
      trace.setSourceType(item.sourceType());
      trace.setSourceId(item.sourceId());
      trace.setPageNumber(item.pageNumber());
      trace.setSourceSection(item.sourceSection());
      trace.setCitationQuality(item.citationQuality());
      trace.setPermissionTrimmed(guardrailOutcome.permissionTrimmed());
      trace.setPiiMasked(guardrailOutcome.piiMasked());
      trace.setPromptInjectionFlagged(guardrailOutcome.promptInjectionDetected());
      trace.setRetrievalPath(item.retrievalPath());
      trace.setFinalScore(scoreFromConfidence(item.citationQuality()));
      aiRetrievalTraceRepository.save(trace);
    }
  }

  private static double scoreFromConfidence(String confidence) {
    if (confidence == null) {
      return 0.5d;
    }
    return switch (confidence.toUpperCase()) {
      case "HIGH" -> 0.95d;
      case "MEDIUM" -> 0.75d;
      case "LOW" -> 0.45d;
      default -> 0.5d;
    };
  }
}
