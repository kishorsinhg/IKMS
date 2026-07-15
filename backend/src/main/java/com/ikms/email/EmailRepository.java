package com.ikms.email;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailRepository extends JpaRepository<Email, UUID> {

  java.util.List<Email> findByClient_IdOrderByReceivedAtDesc(UUID clientId);

  java.util.List<Email> findByClient_IdAndThreadIdOrderByReceivedAtDesc(UUID clientId, String threadId);
}
