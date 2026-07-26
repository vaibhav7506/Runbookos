package com.vaibhav.runbookos.workflow;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowCallbackNonceRepository
    extends JpaRepository<WorkflowCallbackNonce, UUID> {
  boolean existsByNonce(String nonce);
}
