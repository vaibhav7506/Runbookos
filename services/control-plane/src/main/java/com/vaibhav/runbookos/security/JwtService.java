package com.vaibhav.runbookos.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.vaibhav.runbookos.config.RunbookOsProperties;
import com.vaibhav.runbookos.exception.AuthenticationDomainException;
import com.vaibhav.runbookos.identity.Role;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Issues and verifies short-lived HS256 access tokens. */
@Service
public class JwtService {
  private final RunbookOsProperties.Security properties;
  private final byte[] key;

  public JwtService(RunbookOsProperties properties) {
    this.properties = properties.security();
    this.key = properties.security().jwtSecret().getBytes(StandardCharsets.UTF_8);
    if (key.length < 32) {
      throw new IllegalArgumentException("JWT_SECRET must contain at least 32 UTF-8 bytes");
    }
  }

  public String issue(UUID userId, UUID organizationId, Role role, Instant now) {
    try {
      JWTClaimsSet.Builder claims =
          new JWTClaimsSet.Builder()
              .subject(userId.toString())
              .issuer(properties.jwtIssuer())
              .issueTime(Date.from(now))
              .expirationTime(Date.from(now.plus(properties.accessTokenTtl())))
              .jwtID(UUID.randomUUID().toString());
      if (organizationId != null && role != null) {
        claims.claim("org", organizationId.toString()).claim("role", role.name());
      }
      SignedJWT jwt =
          new SignedJWT(new com.nimbusds.jose.JWSHeader(JWSAlgorithm.HS256), claims.build());
      jwt.sign(new MACSigner(key));
      return jwt.serialize();
    } catch (JOSEException ex) {
      throw new IllegalStateException("Could not sign access token", ex);
    }
  }

  public AuthenticatedUser verify(String token, Instant now) {
    try {
      SignedJWT jwt = SignedJWT.parse(token);
      JWTClaimsSet claims = jwt.getJWTClaimsSet();
      if (!jwt.verify(new MACVerifier(key))
          || !properties.jwtIssuer().equals(claims.getIssuer())
          || claims.getExpirationTime() == null
          || !claims.getExpirationTime().toInstant().isAfter(now)) {
        throw AuthenticationDomainException.invalidCredentials();
      }
      UUID organizationId =
          claims.getStringClaim("org") == null
              ? null
              : UUID.fromString(claims.getStringClaim("org"));
      Role role =
          claims.getStringClaim("role") == null
              ? null
              : Role.valueOf(claims.getStringClaim("role"));
      return new AuthenticatedUser(UUID.fromString(claims.getSubject()), organizationId, role);
    } catch (ParseException | JOSEException | IllegalArgumentException ex) {
      throw AuthenticationDomainException.invalidCredentials();
    }
  }
}
