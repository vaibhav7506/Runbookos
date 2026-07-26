package com.vaibhav.runbookos.incident;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {
  Optional<WebhookDelivery> findByOrganizationIdAndSourceAndDeliveryId(
      UUID org, SignalSource source, String deliveryId);

  boolean existsByOrganizationIdAndSourceAndNonce(UUID org, SignalSource source, String nonce);
}
