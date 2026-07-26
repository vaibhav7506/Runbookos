package com.vaibhav.runbookos.analysis;

import java.util.List;
import java.util.UUID;

public record AnalysisOutput(
    String summary,
    List<Claim> likelyRootCauses,
    double confidence,
    List<String> affectedComponents,
    List<String> missingInformation,
    List<ActionRecommendation> diagnosticActions,
    List<ActionRecommendation> mitigationActions,
    List<String> riskNotes) {

  public AnalysisOutput {
    likelyRootCauses = List.copyOf(likelyRootCauses);
    affectedComponents = List.copyOf(affectedComponents);
    missingInformation = List.copyOf(missingInformation);
    diagnosticActions = List.copyOf(diagnosticActions);
    mitigationActions = List.copyOf(mitigationActions);
    riskNotes = List.copyOf(riskNotes);
  }

  public record Claim(String statement, List<UUID> evidenceIds, boolean hypothesis) {
    public Claim {
      evidenceIds = List.copyOf(evidenceIds);
    }
  }

  /**
   * Recommendations are descriptive only. They deliberately contain no executable tool payload,
   * command, URL, or authorization decision.
   */
  public record ActionRecommendation(
      String title, String rationale, List<UUID> evidenceIds, boolean hypothesis) {
    public ActionRecommendation {
      evidenceIds = List.copyOf(evidenceIds);
    }
  }
}
