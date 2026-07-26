package com.vaibhav.runbookos.approval;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class ApprovalExpirationJob {
  private final ApprovalService service;

  public ApprovalExpirationJob(ApprovalService service) {
    this.service = service;
  }

  @Scheduled(fixedDelayString = "${runbookos.approvals.expiry-poll-delay:30000}")
  public void expire() {
    service.expirePending();
  }
}
