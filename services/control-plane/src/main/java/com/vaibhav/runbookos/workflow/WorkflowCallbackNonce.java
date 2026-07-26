package com.vaibhav.runbookos.workflow;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_callback_nonces")
public class WorkflowCallbackNonce {
  @Id private UUID id;

  @Column(name = "execution_id", nullable = false)
  private UUID executionId;

  @Column(nullable = false, length = 128)
  private String nonce;

  @Column(name = "received_at", nullable = false)
  private Instant receivedAt;

  protected WorkflowCallbackNonce() {}

  public static WorkflowCallbackNonce create(UUID execution, String nonce, Instant now) {
    WorkflowCallbackNonce v = new WorkflowCallbackNonce();
    v.id = UUID.randomUUID();
    v.executionId = execution;
    v.nonce = nonce;
    v.receivedAt = now;
    return v;
  }
}
