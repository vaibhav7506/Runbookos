package com.vaibhav.runbookos.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.vaibhav.runbookos.config.RunbookOsProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class HmacSignerTest {
  private final HmacSigner signer =
      new HmacSigner(
          new RunbookOsProperties(
              new RunbookOsProperties.Security(
                  "0123456789012345678901234567890123456789012345678901234567890123",
                  "runbookos",
                  Duration.ofMinutes(15),
                  Duration.ofDays(14),
                  "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                  "primary",
                  "refresh",
                  false,
                  10),
              new RunbookOsProperties.N8n(
                  "http://localhost:5678",
                  "01234567890123456789012345678901",
                  Duration.ofSeconds(30),
                  Duration.ofSeconds(10)),
              new RunbookOsProperties.Cors(List.of("http://localhost:3000"))));

  @Test
  void verifiesOnlyTheExactSignedPayload() {
    String payload = "timestamp.nonce.body";
    String signature = signer.sign(payload);

    assertThat(signer.verify(payload, signature)).isTrue();
    assertThat(signer.verify(payload + "tampered", signature)).isFalse();
    assertThat(signer.verify(payload, null)).isFalse();
  }
}
