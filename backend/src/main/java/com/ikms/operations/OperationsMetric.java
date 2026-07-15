package com.ikms.operations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "operations_metric")
public class OperationsMetric {

  @Id
  private UUID id;

  @Column(name = "metric_group", nullable = false, length = 64)
  private String metricGroup;

  @Column(name = "metric_key", nullable = false, length = 120)
  private String metricKey;

  @Column(name = "metric_value", nullable = false, precision = 18, scale = 4)
  private BigDecimal metricValue;

  @Column(name = "metric_unit", length = 32)
  private String metricUnit;

  @Column(name = "dimensions_json", columnDefinition = "TEXT")
  private String dimensionsJson;

  @Column(name = "recorded_at", nullable = false)
  private Instant recordedAt;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    recordedAt = recordedAt == null ? Instant.now() : recordedAt;
  }

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getMetricGroup() { return metricGroup; }
  public void setMetricGroup(String metricGroup) { this.metricGroup = metricGroup; }
  public String getMetricKey() { return metricKey; }
  public void setMetricKey(String metricKey) { this.metricKey = metricKey; }
  public BigDecimal getMetricValue() { return metricValue; }
  public void setMetricValue(BigDecimal metricValue) { this.metricValue = metricValue; }
  public String getMetricUnit() { return metricUnit; }
  public void setMetricUnit(String metricUnit) { this.metricUnit = metricUnit; }
  public String getDimensionsJson() { return dimensionsJson; }
  public void setDimensionsJson(String dimensionsJson) { this.dimensionsJson = dimensionsJson; }
  public Instant getRecordedAt() { return recordedAt; }
  public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
}

