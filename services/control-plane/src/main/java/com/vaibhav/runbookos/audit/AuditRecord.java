package com.vaibhav.runbookos.audit;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Description of an event to be recorded. Built through {@link #builder(String, String)} so callers
 * cannot accidentally omit a required field, and so the recording API stays stable as fields are
 * added in later phases.
 */
public final class AuditRecord {

  private final UUID organizationId;
  private final UUID actorUserId;
  private final ActorType actorType;
  private final String actorLabel;
  private final String action;
  private final String resourceType;
  private final String resourceId;
  private final AuditOutcome outcome;
  private final String reason;
  private final String ipAddress;
  private final Map<String, Object> metadata;
  private final Instant occurredAt;

  private AuditRecord(Builder builder) {
    this.organizationId = builder.organizationId;
    this.actorUserId = builder.actorUserId;
    this.actorType = builder.actorType;
    this.actorLabel = builder.actorLabel;
    this.action = builder.action;
    this.resourceType = builder.resourceType;
    this.resourceId = builder.resourceId;
    this.outcome = builder.outcome;
    this.reason = builder.reason;
    this.ipAddress = builder.ipAddress;
    this.metadata = Map.copyOf(builder.metadata);
    this.occurredAt = builder.occurredAt;
  }

  /**
   * @param action one of the constants in {@link AuditActions}
   * @param resourceType the kind of thing acted upon, for example {@code organization}
   */
  public static Builder builder(String action, String resourceType) {
    return new Builder(action, resourceType);
  }

  UUID organizationId() {
    return organizationId;
  }

  UUID actorUserId() {
    return actorUserId;
  }

  ActorType actorType() {
    return actorType;
  }

  String actorLabel() {
    return actorLabel;
  }

  String action() {
    return action;
  }

  String resourceType() {
    return resourceType;
  }

  String resourceId() {
    return resourceId;
  }

  AuditOutcome outcome() {
    return outcome;
  }

  String reason() {
    return reason;
  }

  String ipAddress() {
    return ipAddress;
  }

  Map<String, Object> metadata() {
    return metadata;
  }

  Instant occurredAt() {
    return occurredAt;
  }

  /** Mutable builder; not thread safe, intended to be used and discarded within one call. */
  public static final class Builder {

    private final String action;
    private final String resourceType;
    private final Map<String, Object> metadata = new HashMap<>();
    private UUID organizationId;
    private UUID actorUserId;
    private ActorType actorType = ActorType.USER;
    private String actorLabel;
    private String resourceId;
    private AuditOutcome outcome = AuditOutcome.SUCCESS;
    private String reason;
    private String ipAddress;
    private Instant occurredAt;

    private Builder(String action, String resourceType) {
      this.action = action;
      this.resourceType = resourceType;
    }

    /** Null for events that happen before an organization context exists, such as signup. */
    public Builder organization(UUID organizationId) {
      this.organizationId = organizationId;
      return this;
    }

    public Builder actor(UUID actorUserId, String actorLabel) {
      this.actorUserId = actorUserId;
      this.actorLabel = actorLabel;
      this.actorType = ActorType.USER;
      return this;
    }

    public Builder actorType(ActorType actorType) {
      this.actorType = actorType;
      return this;
    }

    public Builder actorLabel(String actorLabel) {
      this.actorLabel = actorLabel;
      return this;
    }

    public Builder resourceId(UUID resourceId) {
      this.resourceId = resourceId == null ? null : resourceId.toString();
      return this;
    }

    public Builder resourceId(String resourceId) {
      this.resourceId = resourceId;
      return this;
    }

    public Builder outcome(AuditOutcome outcome) {
      this.outcome = outcome;
      return this;
    }

    /** Human-readable justification, required for denials and failures. */
    public Builder reason(String reason) {
      this.reason = reason;
      return this;
    }

    public Builder ipAddress(String ipAddress) {
      this.ipAddress = ipAddress;
      return this;
    }

    /**
     * Adds one non-sensitive context value. Never pass secret material, tokens, or password hashes:
     * audit rows are immutable, so a leaked value here cannot be deleted.
     */
    public Builder metadata(String key, Object value) {
      if (value != null) {
        this.metadata.put(key, value);
      }
      return this;
    }

    /** Overrides the timestamp; normally left unset so the service stamps it. */
    public Builder occurredAt(Instant occurredAt) {
      this.occurredAt = occurredAt;
      return this;
    }

    public AuditRecord build() {
      return new AuditRecord(this);
    }
  }
}
