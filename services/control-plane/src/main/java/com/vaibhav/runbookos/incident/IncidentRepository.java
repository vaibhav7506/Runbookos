package com.vaibhav.runbookos.incident;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {
  Optional<Incident> findByIdAndOrganizationId(UUID id, UUID organizationId);

  Optional<Incident>
      findFirstByOrganizationIdAndFingerprintAndStatusNotInAndLastSignalAtAfterOrderByLastSignalAtDesc(
          UUID org,
          String fingerprint,
          java.util.Collection<IncidentStatus> excluded,
          Instant after);

  @Query(
      """
      select i from Incident i where i.organizationId=:org
      and (:status is null or i.status=:status) and (:severity is null or i.severity=:severity)
      and (:service is null or i.affectedService=:service)
      and (:cursor is null or i.lastSignalAt < :cursor)
      and (:query is null or lower(i.title) like lower(concat('%',:query,'%')) or lower(i.affectedService) like lower(concat('%',:query,'%')))
      order by i.lastSignalAt desc, i.id desc
      """)
  Slice<Incident> search(
      @Param("org") UUID org,
      @Param("status") IncidentStatus status,
      @Param("severity") IncidentSeverity severity,
      @Param("service") String service,
      @Param("query") String query,
      @Param("cursor") Instant cursor,
      Pageable pageable);
}
