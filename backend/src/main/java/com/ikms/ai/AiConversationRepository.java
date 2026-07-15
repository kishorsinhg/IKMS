package com.ikms.ai;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiConversationRepository extends JpaRepository<AiConversation, UUID> {

  java.util.List<AiConversation> findByClientIdOrderByCreatedAtDesc(UUID clientId);
}
