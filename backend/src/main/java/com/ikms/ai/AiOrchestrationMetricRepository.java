package com.ikms.ai;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiOrchestrationMetricRepository extends JpaRepository<AiOrchestrationMetric, UUID> {
}
