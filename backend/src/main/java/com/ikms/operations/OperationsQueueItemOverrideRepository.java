package com.ikms.operations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationsQueueItemOverrideRepository extends JpaRepository<OperationsQueueItemOverride, UUID> {

  List<OperationsQueueItemOverride> findByQueueKey(String queueKey);

  Optional<OperationsQueueItemOverride> findByQueueKeyAndItemId(String queueKey, String itemId);
}

