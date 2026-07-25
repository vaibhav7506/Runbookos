package com.vaibhav.runbookos.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A global principal. Organization access is granted through {@link Membership}. */
@Entity
@Table(name = "users")
public class User {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(nullable = false, length = 320)
  private String email;

  /** Lowercased email; the uniqueness and lookup key. */
  @Column(name = "email_normalized", nullable = false, length = 320)
  private String emailNormalized;

  /** BCrypt hash. Never logged, never serialized into a DTO. */
  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(name = "display_name", nullable = false, length = 120)
  private String displayName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private UserStatus status;

  @Column(name = "last_login_at")
  private Instant lastLoginAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(nullable = false)
  private long version;

  protected User() {
    // Required by JPA.
  }

  private User(
      UUID id,
      String email,
      String passwordHash,
      String displayName,
      UserStatus status,
      Instant now) {
    this.id = id;
    this.email = email;
    this.emailNormalized = normalizeEmail(email);
    this.passwordHash = passwordHash;
    this.displayName = displayName;
    this.status = status;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public static User create(String email, String passwordHash, String displayName, Instant now) {
    return new User(UUID.randomUUID(), email, passwordHash, displayName, UserStatus.ACTIVE, now);
  }

  /** Canonical email form used for uniqueness. Kept in one place so signup and login agree. */
  public static String normalizeEmail(String email) {
    return email.trim().toLowerCase(java.util.Locale.ROOT);
  }

  public void recordLogin(Instant at) {
    this.lastLoginAt = at;
    this.updatedAt = at;
  }

  public void changePassword(String newPasswordHash, Instant at) {
    this.passwordHash = newPasswordHash;
    this.updatedAt = at;
  }

  public void rename(String newDisplayName, Instant at) {
    this.displayName = newDisplayName;
    this.updatedAt = at;
  }

  public boolean isActive() {
    return status == UserStatus.ACTIVE;
  }

  public UUID getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getEmailNormalized() {
    return emailNormalized;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getDisplayName() {
    return displayName;
  }

  public UserStatus getStatus() {
    return status;
  }

  public Instant getLastLoginAt() {
    return lastLoginAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public long getVersion() {
    return version;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    return other instanceof User user && id != null && id.equals(user.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  /** Deliberately excludes email and password hash to keep them out of logs. */
  @Override
  public String toString() {
    return "User[id=%s, status=%s]".formatted(id, status);
  }
}
