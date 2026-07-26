package com.vaibhav.runbookos.incident;

import com.vaibhav.runbookos.common.TimeProvider;
import com.vaibhav.runbookos.exception.AuthenticationDomainException;
import com.vaibhav.runbookos.exception.ConflictException;
import com.vaibhav.runbookos.security.HmacSigner;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class WebhookService {
  public static final int MAX_PAYLOAD_BYTES = 256 * 1024;
  private final WebhookDeliveryRepository deliveries;
  private final IncidentService incidents;
  private final PayloadSanitizer sanitizer;
  private final HmacSigner signer;
  private final ObjectMapper mapper;
  private final TimeProvider time;

  public WebhookService(
      WebhookDeliveryRepository deliveries,
      IncidentService incidents,
      PayloadSanitizer sanitizer,
      HmacSigner signer,
      ObjectMapper mapper,
      TimeProvider time) {
    this.deliveries = deliveries;
    this.incidents = incidents;
    this.sanitizer = sanitizer;
    this.signer = signer;
    this.mapper = mapper;
    this.time = time;
  }

  @Transactional
  public Incident accept(
      UUID org,
      SignalSource source,
      String deliveryId,
      String nonce,
      String timestamp,
      String signature,
      byte[] body) {
    if (body.length > MAX_PAYLOAD_BYTES)
      throw new ConflictException("WEBHOOK_TOO_LARGE", "Webhook payload exceeds 256 KiB");
    if (deliveryId == null || deliveryId.isBlank() || nonce == null || nonce.isBlank())
      throw new ConflictException("WEBHOOK_HEADERS_REQUIRED", "Delivery ID and nonce are required");
    var duplicate = deliveries.findByOrganizationIdAndSourceAndDeliveryId(org, source, deliveryId);
    if (duplicate.isPresent()) return incidents.require(org, duplicate.get().getIncidentId());
    Instant sentAt;
    try {
      sentAt = Instant.ofEpochSecond(Long.parseLong(timestamp));
    } catch (Exception ex) {
      throw invalidSignature();
    }
    Instant now = time.nowTruncated();
    if (Duration.between(sentAt, now).abs().compareTo(Duration.ofMinutes(5)) > 0)
      throw new AuthenticationDomainException(
          "WEBHOOK_TIMESTAMP_INVALID", "Webhook timestamp is outside the accepted tolerance");
    if (deliveries.existsByOrganizationIdAndSourceAndNonce(org, source, nonce))
      throw new AuthenticationDomainException(
          "WEBHOOK_REPLAY_DETECTED", "Webhook nonce has already been used");
    String text = new String(body, StandardCharsets.UTF_8);
    if (!signer.verify(timestamp + "." + nonce + "." + text, stripPrefix(signature)))
      throw invalidSignature();
    Map<String, Object> payload;
    try {
      payload = mapper.readValue(body, new TypeReference<>() {});
    } catch (Exception ex) {
      throw new ConflictException(
          "INVALID_WEBHOOK_PAYLOAD", "Webhook payload must be a JSON object");
    }
    Map<String, Object> safe = sanitizer.sanitize(payload);
    Incident incident = incidents.ingest(org, source, deliveryId, safe);
    deliveries.save(
        WebhookDelivery.create(
            org, source, deliveryId, nonce, sha256(body), incident.getId(), now));
    return incident;
  }

  private static AuthenticationDomainException invalidSignature() {
    return new AuthenticationDomainException(
        "INVALID_WEBHOOK_SIGNATURE", "Webhook signature is invalid");
  }

  private static String stripPrefix(String signature) {
    return signature != null && signature.startsWith("sha256=")
        ? signature.substring(7)
        : signature;
  }

  private static String sha256(byte[] body) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
