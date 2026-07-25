package com.vaibhav.runbookos.security;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {

  Optional<RefreshSession> findByTokenHash(String tokenHash);

  /**
   * Revokes every live session for a user. Used on logout-everywhere, password change, and on
   * detection of a replayed refresh token, where the safe response is to end the whole chain.
   */
  @Modifying
  @Query(
      """
      update RefreshSession s
      set s.revokedAt = :at, s.revokedReason = :reason
      where s.userId = :userId and s.revokedAt is null
      """)
  int revokeAllForUser(
      @Param("userId") UUID userId, @Param("at") Instant at, @Param("reason") String reason);

  /** Housekeeping for expired rows; retention is enforced by the Phase 8 cleanup job. */
  @Modifying
  @Query("delete from RefreshSession s where s.expiresAt < :before")
  int deleteExpiredBefore(@Param("before") Instant before);
}
