package com.ikms.ai;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiConversationMessageRepository extends JpaRepository<AiConversationMessage, UUID> {

  List<AiConversationMessage> findTop12ByConversationIdOrderByCreatedAtDesc(UUID conversationId);

  long countByConversationId(UUID conversationId);
}
