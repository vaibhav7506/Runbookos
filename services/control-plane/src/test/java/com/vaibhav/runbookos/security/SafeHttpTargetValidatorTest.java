package com.vaibhav.runbookos.security;

import static org.assertj.core.api.Assertions.*;

import java.net.URI;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SafeHttpTargetValidatorTest {
  private final SafeHttpTargetValidator validator = new SafeHttpTargetValidator();

  @Test
  void rejectsLoopbackPrivateAndLinkLocalTargets() {
    assertThatThrownBy(() -> validator.validate("https://127.0.0.1/health", Set.of()))
        .isInstanceOf(SafeHttpTargetValidator.UnsafeTargetException.class);
    assertThatThrownBy(() -> validator.validate("https://10.0.0.1/health", Set.of()))
        .isInstanceOf(SafeHttpTargetValidator.UnsafeTargetException.class);
    assertThatThrownBy(() -> validator.validate("https://169.254.169.254/latest", Set.of()))
        .isInstanceOf(SafeHttpTargetValidator.UnsafeTargetException.class);
    assertThatThrownBy(() -> validator.validate("https://[::1]/health", Set.of()))
        .isInstanceOf(SafeHttpTargetValidator.UnsafeTargetException.class);
  }

  @Test
  void privateTargetRequiresAnExactExplicitAllowlistEntry() {
    URI accepted = validator.validate("https://10.0.0.1/health", Set.of("10.0.0.1"));
    assertThat(accepted.getHost()).isEqualTo("10.0.0.1");
    assertThatThrownBy(
            () -> validator.validate("https://10.0.0.1/health", Set.of("internal.example")))
        .isInstanceOf(SafeHttpTargetValidator.UnsafeTargetException.class);
  }

  @Test
  void validatesRedirectTargetsIndependently() {
    assertThatThrownBy(
            () ->
                validator.validateRedirect(
                    URI.create("https://example.com/health"), "https://127.0.0.1/admin", Set.of()))
        .isInstanceOf(SafeHttpTargetValidator.UnsafeTargetException.class);
  }

  @Test
  void rejectsCredentialsAndNonTlsSchemes() {
    assertThatThrownBy(() -> validator.validate("http://example.com", Set.of()))
        .isInstanceOf(SafeHttpTargetValidator.UnsafeTargetException.class);
    assertThatThrownBy(() -> validator.validate("https://user:pass@example.com", Set.of()))
        .isInstanceOf(SafeHttpTargetValidator.UnsafeTargetException.class);
  }
}
