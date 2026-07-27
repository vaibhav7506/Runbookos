package com.vaibhav.runbookos.approval;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Version;
import org.junit.jupiter.api.Test;

class ApprovalConcurrencyGuardTest {
  @Test
  void sensitiveApprovalTransitionUsesJpaOptimisticLocking() throws Exception {
    assertThat(ApprovalRequest.class.getDeclaredField("version").isAnnotationPresent(Version.class))
        .isTrue();
  }
}
