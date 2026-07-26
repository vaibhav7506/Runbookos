package com.vaibhav.runbookos.workflow;

import com.vaibhav.runbookos.common.TimeProvider;
import com.vaibhav.runbookos.config.RunbookOsProperties;
import com.vaibhav.runbookos.execution.*;
import com.vaibhav.runbookos.security.HmacSigner;
import java.util.*;
import org.slf4j.*;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Component
@Profile("!test")
public class OutboxProcessor {
  private static final Logger log = LoggerFactory.getLogger(OutboxProcessor.class);
  private final OutboxEventRepository outbox;
  private final ExecutionRepository executions;
  private final ExecutionTokenService tokens;
  private final HmacSigner signer;
  private final ObjectMapper mapper;
  private final TimeProvider time;
  private final RestClient client;

  public OutboxProcessor(
      OutboxEventRepository outbox,
      ExecutionRepository executions,
      ExecutionTokenService tokens,
      HmacSigner signer,
      ObjectMapper mapper,
      TimeProvider time,
      RunbookOsProperties properties,
      RestClient.Builder builder) {
    this.outbox = outbox;
    this.executions = executions;
    this.tokens = tokens;
    this.signer = signer;
    this.mapper = mapper;
    this.time = time;
    this.client = builder.baseUrl(properties.n8n().baseUrl()).build();
  }

  @Scheduled(fixedDelayString = "${runbookos.n8n.outbox-poll-delay:1000}")
  @Transactional
  public void process() {
    for (OutboxEvent event : outbox.findReady(time.now(), PageRequest.of(0, 20))) {
      event.claim(time.nowTruncated());
      try {
        dispatch(event);
        event.delivered(time.nowTruncated());
      } catch (Exception ex) {
        log.warn(
            "Workflow dispatch failed eventId={} attempt={}", event.getId(), event.getAttempts());
        event.failed(ex.getMessage(), time.nowTruncated());
      }
    }
  }

  private void dispatch(OutboxEvent event) throws Exception {
    if ("approval".equals(event.getAggregateType())) {
      dispatchApproval(event);
      return;
    }
    Execution execution = executions.findById(event.getAggregateId()).orElseThrow();
    String executionToken =
        tokens.issue(
            execution.getId(),
            execution.getOrganizationId(),
            execution.getAllowedActionIds(),
            execution.getTokenNonce());
    DispatchRequest request =
        new DispatchRequest(
            event.getId(),
            execution.getId(),
            execution.getOrganizationId(),
            execution.getWorkflowKey(),
            execution.getWorkflowVersion(),
            executionToken,
            event.getEventType(),
            event.getPayload());
    String body = mapper.writeValueAsString(request);
    String timestamp = Long.toString(time.now().getEpochSecond());
    String nonce = UUID.randomUUID().toString();
    client
        .post()
        .uri("/webhook/runbookos-execute")
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-Internal-Timestamp", timestamp)
        .header("X-Internal-Nonce", nonce)
        .header("X-Internal-Signature", signer.sign(timestamp + "." + nonce + "." + body))
        .body(body)
        .retrieve()
        .toBodilessEntity();
  }

  private void dispatchApproval(OutboxEvent event) throws Exception {
    ApprovalDispatchRequest request =
        new ApprovalDispatchRequest(
            event.getId(),
            event.getAggregateId(),
            event.getOrganizationId(),
            event.getEventType(),
            event.getPayload());
    String body = mapper.writeValueAsString(request);
    String timestamp = Long.toString(time.now().getEpochSecond());
    String nonce = UUID.randomUUID().toString();
    client
        .post()
        .uri("/webhook/runbookos-approval")
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-Internal-Timestamp", timestamp)
        .header("X-Internal-Nonce", nonce)
        .header("X-Internal-Signature", signer.sign(timestamp + "." + nonce + "." + body))
        .body(body)
        .retrieve()
        .toBodilessEntity();
  }

  public record DispatchRequest(
      UUID dispatchId,
      UUID executionId,
      UUID organizationId,
      String workflowKey,
      int workflowVersion,
      String executionToken,
      String eventType,
      Map<String, Object> payload) {}

  public record ApprovalDispatchRequest(
      UUID dispatchId,
      UUID approvalId,
      UUID organizationId,
      String eventType,
      Map<String, Object> payload) {}
}
