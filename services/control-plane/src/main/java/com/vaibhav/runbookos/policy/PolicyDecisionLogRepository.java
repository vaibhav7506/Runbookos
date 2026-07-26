package com.vaibhav.runbookos.policy;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyDecisionLogRepository extends JpaRepository<PolicyDecisionLog, UUID> {}
