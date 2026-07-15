package com.ikms.operations;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationsJobRepository extends JpaRepository<OperationsJob, UUID> {

  List<OperationsJob> findAllByOrderBySubmittedAtDesc();

  List<OperationsJob> findByQueueKeyOrderByPriorityDescSubmittedAtAsc(String queueKey);
}

