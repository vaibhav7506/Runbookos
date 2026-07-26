package com.vaibhav.runbookos.approval;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalCallbackNonceRepository
    extends JpaRepository<ApprovalCallbackNonce, UUID> {
  boolean existsByNonce(String nonce);
}
