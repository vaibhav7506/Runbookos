package com.vaibhav.runbookos.approval;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {
  long countByOrganizationIdAndStatus(UUID organizationId, ApprovalStatus status);

  Optional<ApprovalRequest> findByIdAndOrganizationId(UUID id, UUID organizationId);

  List<ApprovalRequest> findByOrganizationIdOrderByRequestedAtDesc(UUID organizationId);

  List<ApprovalRequest> findByOrganizationIdAndStatusOrderByRequestedAtDesc(
      UUID organizationId, ApprovalStatus status);

  List<ApprovalRequest> findByStatusAndExpiresAtBefore(ApprovalStatus status, Instant now);

  List<ApprovalRequest> findByOrganizationIdAndIncidentIdAndStatus(
      UUID organizationId, UUID incidentId, ApprovalStatus status);
}
