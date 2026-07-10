package com.ikms.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

  @Query("""
      select auditLog
      from AuditLog auditLog
      where (:actorUsername is null or lower(coalesce(auditLog.actorUsername, '')) like lower(concat('%', :actorUsername, '%')))
        and (:action is null or lower(auditLog.action) like lower(concat('%', :action, '%')))
        and (:clientId is null or auditLog.clientId = :clientId)
        and (:fromTime is null or auditLog.occurredAt >= :fromTime)
        and (:toTime is null or auditLog.occurredAt <= :toTime)
      order by auditLog.occurredAt desc
      """)
  List<AuditLog> search(
      @Param("actorUsername") String actorUsername,
      @Param("action") String action,
      @Param("clientId") UUID clientId,
      @Param("fromTime") Instant fromTime,
      @Param("toTime") Instant toTime,
      Pageable pageable);
}
