package com.vaibhav.runbookos.incident;

import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/{organizationId}")
public class WebhookController {
  private final WebhookService service;

  public WebhookController(WebhookService service) {
    this.service = service;
  }

  @PostMapping(value = "/{source}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public WebhookResponse receive(
      @PathVariable UUID organizationId,
      @PathVariable String source,
      @RequestHeader("X-Delivery-ID") String deliveryId,
      @RequestHeader("X-Webhook-Nonce") String nonce,
      @RequestHeader("X-Webhook-Timestamp") String timestamp,
      @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
      @RequestHeader(value = "X-Hub-Signature-256", required = false) String githubSignature,
      @RequestBody byte[] body) {
    SignalSource kind =
        switch (source.toLowerCase(java.util.Locale.ROOT)) {
          case "sentry" -> SignalSource.SENTRY;
          case "github" -> SignalSource.GITHUB;
          case "custom" -> SignalSource.CUSTOM;
          default ->
              throw new com.vaibhav.runbookos.exception.ConflictException(
                  "UNSUPPORTED_WEBHOOK_SOURCE", "Unsupported webhook source");
        };
    Incident incident =
        service.accept(
            organizationId,
            kind,
            deliveryId,
            nonce,
            timestamp,
            signature != null ? signature : githubSignature,
            body);
    return new WebhookResponse(incident.getId(), incident.getStatus(), incident.getSignalCount());
  }

  public record WebhookResponse(UUID incidentId, IncidentStatus status, int signalCount) {}
}
