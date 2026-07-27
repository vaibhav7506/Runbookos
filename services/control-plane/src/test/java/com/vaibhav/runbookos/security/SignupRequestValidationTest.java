package com.vaibhav.runbookos.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class SignupRequestValidationTest {
  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void acceptsPersonalEmailAddresses() {
    var request =
        new AuthController.SignupRequest(
            "responder@gmail.com", "correct-horse-battery", "Personal User");

    assertThat(validator.validate(request)).isEmpty();
  }

  @Test
  void rejectsMissingPasswordWithAFieldMessage() {
    var request = new AuthController.SignupRequest("person@yahoo.com", "", "Personal User");

    assertThat(validator.validate(request))
        .anySatisfy(
            violation -> {
              assertThat(violation.getPropertyPath().toString()).isEqualTo("password");
              assertThat(violation.getMessage()).isEqualTo("Password is required");
            });
  }
}
