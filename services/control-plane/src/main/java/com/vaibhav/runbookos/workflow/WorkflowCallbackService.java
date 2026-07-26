package com.vaibhav.runbookos.workflow;

import com.vaibhav.runbookos.common.TimeProvider;
import com.vaibhav.runbookos.exception.*;
import com.vaibhav.runbookos.execution.*;
import com.vaibhav.runbookos.security.HmacSigner;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class WorkflowCallbackService {
  private final ExecutionRepository executions;
  private final StepExecutionRepository steps;
  private final WorkflowCallbackNonceRepository nonces;
  private final ExecutionTokenService tokens;
  private final HmacSigner signer;
  private final ObjectMapper mapper;
  private final TimeProvider time;
  private final ExecutionService service;

  public WorkflowCallbackService(
      ExecutionRepository executions,
      StepExecutionRepository steps,
      WorkflowCallbackNonceRepository nonces,
      ExecutionTokenService tokens,
      HmacSigner signer,
      ObjectMapper mapper,
      TimeProvider time,
      ExecutionService service) {
    this.executions = executions;
    this.steps = steps;
    this.nonces = nonces;
    this.tokens = tokens;
    this.signer = signer;
    this.mapper = mapper;
    this.time = time;
    this.service = service;
  }

  @Transactional
  public void accept(
      String token, String timestamp, String nonce, String signature, byte[] rawBody) {
    String body = new String(rawBody, java.nio.charset.StandardCharsets.UTF_8);
    CallbackRequest request;
    try {
      request = mapper.readValue(rawBody, CallbackRequest.class);
    } catch (Exception e) {
      throw new ConflictException("INVALID_CALLBACK", "Callback could not be read");
    }
    Instant sent;
    try {
      sent = Instant.ofEpochSecond(Long.parseLong(timestamp));
    } catch (Exception e) {
      throw invalid();
    }
    if (Duration.between(sent, time.now()).abs().compareTo(Duration.ofMinutes(2)) > 0
        || !signer.verify(timestamp + "." + nonce + "." + body, signature)) throw invalid();
    if (nonces.existsByNonce(nonce))
      throw new AuthenticationDomainException(
          "CALLBACK_REPLAY_DETECTED", "Callback nonce has already been used");
    var claims = tokens.verify(token);
    if (!claims.executionId().equals(request.executionId())) throw invalid();
    Execution execution =
        executions
            .findByIdAndOrganizationId(request.executionId(), claims.organizationId())
            .orElseThrow(() -> new ResourceNotFoundException("execution", request.executionId()));
    if (request.actionId() != null && !execution.getAllowedActionIds().contains(request.actionId()))
      throw new AccessDeniedDomainException(
          "Callback action is not authorized by the execution token");
    nonces.save(WorkflowCallbackNonce.create(execution.getId(), nonce, time.nowTruncated()));
    if (request.actionId() != null) {
      StepExecution step =
          steps
              .findFirstByExecutionIdAndActionIdOrderByAttemptDesc(
                  execution.getId(), request.actionId())
              .orElseThrow(() -> new ResourceNotFoundException("step", request.actionId()));
      step.update(
          request.stepStatus(),
          request.output(),
          request.errorCode(),
          request.errorMessage(),
          time.nowTruncated());
      service.recordEvent(
          execution,
          step,
          "STEP_UPDATED",
          step.getStatus().name(),
          request.message() == null ? step.getName() + " updated" : request.message(),
          Map.of("actionId", request.actionId(), "attempt", step.getAttempt()));
    }
    if (request.executionStatus() != null && request.executionStatus() != execution.getStatus()) {
      ExecutionStatus from = execution.getStatus();
      execution.transition(request.executionStatus(), time.nowTruncated());
      service.recordEvent(
          execution,
          null,
          "EXECUTION_UPDATED",
          execution.getStatus().name(),
          request.message() == null ? "Execution state updated" : request.message(),
          Map.of("from", from.name()));
    }
  }

  private static AuthenticationDomainException invalid() {
    return new AuthenticationDomainException(
        "INVALID_WORKFLOW_CALLBACK", "Workflow callback authentication failed");
  }

  public record CallbackRequest(
      UUID executionId,
      String actionId,
      StepStatus stepStatus,
      ExecutionStatus executionStatus,
      String message,
      String errorCode,
      String errorMessage,
      Map<String, Object> output) {}
}
