package com.ikms.operations;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationsMetricRepository extends JpaRepository<OperationsMetric, UUID> {

  List<OperationsMetric> findTop20ByMetricGroupOrderByRecordedAtDesc(String metricGroup);
}
