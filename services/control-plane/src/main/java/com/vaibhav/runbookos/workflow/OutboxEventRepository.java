package com.vaibhav.runbookos.workflow;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select o from OutboxEvent o where o.status='PENDING' and o.nextAttemptAt<=:now order by o.createdAt")
  List<OutboxEvent> findReady(@Param("now") Instant now, Pageable pageable);

  List<OutboxEvent> findByOrganizationIdAndStatusOrderByCreatedAtDesc(
      UUID organizationId, OutboxStatus status, Pageable pageable);

  long countByOrganizationIdAndStatus(UUID organizationId, OutboxStatus status);
}
