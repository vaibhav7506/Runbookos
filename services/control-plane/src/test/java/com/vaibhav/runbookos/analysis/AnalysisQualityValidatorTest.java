package com.vaibhav.runbookos.analysis;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class AnalysisQualityValidatorTest {
  private final AnalysisQualityValidator validator = new AnalysisQualityValidator();

  @Test
  void rejectsNonexistentEvidenceReferences() {
    UUID existing = UUID.randomUUID();
    AnalysisOutput output =
        output(new AnalysisOutput.Claim("A factual claim", List.of(UUID.randomUUID()), false));
    assertThatThrownBy(() -> validator.validate(output, Set.of(existing), "SEV2"))
        .isInstanceOf(AnalysisQualityValidator.InvalidAnalysisException.class)
        .hasMessageContaining("INVALID_EVIDENCE_REFERENCE");
  }

  @Test
  void requiresUncitedClaimsToBeHypotheses() {
    AnalysisOutput output =
        output(new AnalysisOutput.Claim("Unsupported conclusion", List.of(), false));
    assertThatThrownBy(() -> validator.validate(output, Set.of(), "SEV3"))
        .hasMessageContaining("MISSING_CITATION");
    assertThat(
            validator
                .validate(
                    output(new AnalysisOutput.Claim("Explicit hypothesis", List.of(), true)),
                    Set.of(),
                    "SEV3")
                .valid())
        .isTrue();
  }

  private static AnalysisOutput output(AnalysisOutput.Claim claim) {
    return new AnalysisOutput(
        "Summary",
        List.of(claim),
        0.7,
        List.of("api"),
        List.of(),
        List.of(
            new AnalysisOutput.ActionRecommendation(
                "Collect evidence", "Close the gap", List.of(), true)),
        List.of(),
        List.of("Advisory"));
  }
}
