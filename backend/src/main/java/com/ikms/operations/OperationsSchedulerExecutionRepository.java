package com.ikms.operations;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationsSchedulerExecutionRepository extends JpaRepository<OperationsSchedulerExecution, UUID> {

  List<OperationsSchedulerExecution> findTop10BySchedulerKeyOrderByStartedAtDesc(String schedulerKey);
}

