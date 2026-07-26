package com.vaibhav.runbookos.audit;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Read and append only. No update or delete method is exposed, and the database rejects both, so
 * the audit trail cannot be rewritten through this interface.
 */
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

  /** Tenant-scoped listing. Every read path for audit data must go through an org-scoped method. */
  Page<AuditEvent> findByOrganizationIdOrderByOccurredAtDesc(
      UUID organizationId, Pageable pageable);

  Page<AuditEvent> findByOrganizationIdAndActionOrderByOccurredAtDesc(
      UUID organizationId, String action, Pageable pageable);

  Page<AuditEvent> findByOrganizationIdAndResourceTypeAndResourceIdOrderByOccurredAtDesc(
      UUID organizationId, String resourceType, String resourceId, Pageable pageable);
}
