package com.vaibhav.runbookos.workflow;

import com.vaibhav.runbookos.execution.ExecutionService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class ExecutionTimeoutJob {
  private final ExecutionService service;

  public ExecutionTimeoutJob(ExecutionService service) {
    this.service = service;
  }

  @Scheduled(fixedDelayString = "${runbookos.n8n.timeout-scan-delay:30000}")
  public void scan() {
    service.timeoutExpired();
  }
}
