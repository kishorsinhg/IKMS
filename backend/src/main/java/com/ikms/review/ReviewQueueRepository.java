package com.ikms.review;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewQueueRepository extends JpaRepository<ReviewQueueItem, UUID> {
}
