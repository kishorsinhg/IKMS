package com.ikms.review;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewQueueRepository extends JpaRepository<ReviewQueueItem, UUID> {

  @Query("""
      select r
      from ReviewQueueItem r
      where (:status is null or r.status = :status)
        and (:reason is null or r.reason = :reason)
      order by r.createdAt desc
      """)
  List<ReviewQueueItem> findByOptionalStatusAndReason(
      @Param("status") ReviewQueueStatus status,
      @Param("reason") ReviewQueueReason reason);

  Optional<ReviewQueueItem> findByItemTypeAndItemIdAndStatusIn(
      ReviewQueueItemType itemType,
      String itemId,
      List<ReviewQueueStatus> statuses);
}
