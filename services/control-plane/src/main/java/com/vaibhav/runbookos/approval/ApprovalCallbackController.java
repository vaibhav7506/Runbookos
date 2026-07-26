package com.vaibhav.runbookos.approval;

import com.vaibhav.runbookos.common.TimeProvider;
import com.vaibhav.runbookos.exception.AuthenticationDomainException;
import com.vaibhav.runbookos.security.HmacSigner;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/internal/workflows/approvals")
public class ApprovalCallbackController {
  private final ApprovalService approvals;
  private final ApprovalCallbackNonceRepository nonces;
  private final HmacSigner signer;
  private final ObjectMapper mapper;
  private final TimeProvider time;

  public ApprovalCallbackController(
      ApprovalService approvals,
      ApprovalCallbackNonceRepository nonces,
      HmacSigner signer,
      ObjectMapper mapper,
      TimeProvider time) {
    this.approvals = approvals;
    this.nonces = nonces;
    this.signer = signer;
    this.mapper = mapper;
    this.time = time;
  }

  @PostMapping("/decisions")
  public ApprovalService.ApprovalView decide(
      @RequestHeader("X-Internal-Timestamp") String timestamp,
      @RequestHeader("X-Internal-Nonce") String nonce,
      @RequestHeader("X-Internal-Signature") String signature,
      @RequestBody byte[] rawBody) {
    String body = new String(rawBody, StandardCharsets.UTF_8);
    Instant sent;
    CallbackRequest request;
    try {
      sent = Instant.ofEpochSecond(Long.parseLong(timestamp));
      request = mapper.readValue(rawBody, CallbackRequest.class);
    } catch (Exception ex) {
      throw invalid();
    }
    if (Duration.between(sent, time.now()).abs().compareTo(Duration.ofMinutes(2)) > 0
        || !signer.verify(timestamp + "." + nonce + "." + body, signature)) throw invalid();
    if (nonces.existsByNonce(nonce))
      throw new AuthenticationDomainException(
          "APPROVAL_CALLBACK_REPLAY", "Approval callback nonce was already used");
    nonces.save(ApprovalCallbackNonce.create(nonce, time.nowTruncated()));
    return approvals.decide(
        request.organizationId(),
        request.approvalId(),
        new ApprovalService.DecisionInput(
            request.decision(), request.reason(), request.confirmationPhrase()),
        request.approverUserId());
  }

  private static AuthenticationDomainException invalid() {
    return new AuthenticationDomainException(
        "INVALID_APPROVAL_CALLBACK", "Approval callback authentication failed");
  }

  public record CallbackRequest(
      UUID organizationId,
      UUID approvalId,
      UUID approverUserId,
      ApprovalChoice decision,
      String reason,
      String confirmationPhrase) {}
}
