package com.ikms.ai;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiInteractionRepository extends JpaRepository<AiInteraction, UUID> {
}
