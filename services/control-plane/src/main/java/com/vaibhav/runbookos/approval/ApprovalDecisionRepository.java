package com.vaibhav.runbookos.approval;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalDecisionRepository extends JpaRepository<ApprovalDecisionRecord, UUID> {
  List<ApprovalDecisionRecord> findByApprovalRequestIdOrderByDecidedAtAsc(UUID requestId);

  Optional<ApprovalDecisionRecord> findByApprovalRequestIdAndApproverUserId(
      UUID requestId, UUID approverId);

  long countByApprovalRequestIdAndDecision(UUID requestId, ApprovalChoice choice);
}
