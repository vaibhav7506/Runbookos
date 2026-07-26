package com.vaibhav.runbookos.approval;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_callback_nonces")
public class ApprovalCallbackNonce {
  @Id private UUID id;

  @Column(nullable = false, length = 128)
  private String nonce;

  @Column(name = "received_at", nullable = false)
  private Instant receivedAt;

  protected ApprovalCallbackNonce() {}

  public static ApprovalCallbackNonce create(String nonce, Instant now) {
    ApprovalCallbackNonce value = new ApprovalCallbackNonce();
    value.id = UUID.randomUUID();
    value.nonce = nonce;
    value.receivedAt = now;
    return value;
  }
}
