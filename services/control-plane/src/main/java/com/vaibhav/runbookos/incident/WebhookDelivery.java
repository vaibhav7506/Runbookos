package com.vaibhav.runbookos.incident;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_deliveries")
public class WebhookDelivery {
  @Id private UUID id;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private SignalSource source;

  @Column(name = "delivery_id", nullable = false, length = 255)
  private String deliveryId;

  @Column(nullable = false, length = 255)
  private String nonce;

  @Column(name = "signature_valid", nullable = false)
  private boolean signatureValid;

  @Column(name = "payload_hash", nullable = false, length = 64)
  private String payloadHash;

  @Column(name = "incident_id")
  private UUID incidentId;

  @Column(name = "received_at", nullable = false)
  private Instant receivedAt;

  protected WebhookDelivery() {}

  public static WebhookDelivery create(
      UUID org,
      SignalSource source,
      String delivery,
      String nonce,
      String hash,
      UUID incident,
      Instant now) {
    WebhookDelivery v = new WebhookDelivery();
    v.id = UUID.randomUUID();
    v.organizationId = org;
    v.source = source;
    v.deliveryId = delivery;
    v.nonce = nonce;
    v.signatureValid = true;
    v.payloadHash = hash;
    v.incidentId = incident;
    v.receivedAt = now;
    return v;
  }

  public UUID getIncidentId() {
    return incidentId;
  }
}
