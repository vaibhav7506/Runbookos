package com.vaibhav.runbookos.incident;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {
  @Id private UUID id;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Column(nullable = false, length = 64)
  private String scope;

  @Column(name = "idempotency_key", nullable = false, length = 255)
  private String key;

  @Column(name = "request_hash", nullable = false, length = 64)
  private String requestHash;

  @Column(name = "resource_id")
  private UUID resourceId;

  @Column(name = "response_status")
  private Integer responseStatus;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  protected IdempotencyRecord() {}

  public static IdempotencyRecord create(
      UUID org,
      String scope,
      String key,
      String hash,
      UUID resource,
      Integer status,
      Instant now,
      Instant expires) {
    IdempotencyRecord v = new IdempotencyRecord();
    v.id = UUID.randomUUID();
    v.organizationId = org;
    v.scope = scope;
    v.key = key;
    v.requestHash = hash;
    v.resourceId = resource;
    v.responseStatus = status;
    v.createdAt = now;
    v.expiresAt = expires;
    return v;
  }
}
