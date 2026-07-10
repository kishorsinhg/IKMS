package com.ikms.email;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailRepository extends JpaRepository<Email, UUID> {
}
