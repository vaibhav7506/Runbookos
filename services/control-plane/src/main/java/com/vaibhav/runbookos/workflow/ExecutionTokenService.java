package com.vaibhav.runbookos.workflow;

import com.vaibhav.runbookos.common.TimeProvider;
import com.vaibhav.runbookos.config.RunbookOsProperties;
import com.vaibhav.runbookos.exception.AuthenticationDomainException;
import com.vaibhav.runbookos.security.HmacSigner;
import java.util.*;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class ExecutionTokenService {
  private final ObjectMapper mapper;
  private final HmacSigner signer;
  private final TimeProvider time;
  private final RunbookOsProperties.N8n properties;

  public ExecutionTokenService(
      ObjectMapper mapper, HmacSigner signer, TimeProvider time, RunbookOsProperties properties) {
    this.mapper = mapper;
    this.signer = signer;
    this.time = time;
    this.properties = properties.n8n();
  }

  public String issue(UUID execution, UUID org, List<String> actions, String nonce) {
    try {
      TokenClaims claims =
          new TokenClaims(
              execution,
              org,
              List.copyOf(actions),
              time.now().plus(properties.tokenTtl()).getEpochSecond(),
              nonce);
      String payload =
          Base64.getUrlEncoder().withoutPadding().encodeToString(mapper.writeValueAsBytes(claims));
      return payload + "." + signer.sign(payload);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public TokenClaims verify(String token) {
    try {
      String[] parts = token.split("\\.");
      if (parts.length != 2 || !signer.verify(parts[0], parts[1])) throw invalid();
      TokenClaims claims =
          mapper.readValue(Base64.getUrlDecoder().decode(parts[0]), TokenClaims.class);
      if (claims.exp() <= time.now().getEpochSecond()) throw invalid();
      return claims;
    } catch (AuthenticationDomainException e) {
      throw e;
    } catch (Exception e) {
      throw invalid();
    }
  }

  private static AuthenticationDomainException invalid() {
    return new AuthenticationDomainException(
        "INVALID_EXECUTION_TOKEN", "Execution token is invalid or expired");
  }

  public record TokenClaims(
      UUID executionId,
      UUID organizationId,
      List<String> allowedActionIds,
      long exp,
      String nonce) {}
}
