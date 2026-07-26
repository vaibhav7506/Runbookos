package com.vaibhav.runbookos.analysis;

import com.vaibhav.runbookos.exception.DomainException;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AnalysisQualityValidator {
  public QualityReport validate(
      AnalysisOutput output, Set<UUID> availableEvidenceIds, String incidentSeverity) {
    List<String> failures = new ArrayList<>();
    if (output.summary() == null || output.summary().isBlank()) failures.add("EMPTY_SUMMARY");
    if (output.confidence() < 0 || output.confidence() > 1) failures.add("INVALID_CONFIDENCE");
    if (output.diagnosticActions().isEmpty() && output.mitigationActions().isEmpty())
      failures.add("EMPTY_RECOMMENDATIONS");

    List<AnalysisOutput.Claim> claims = output.likelyRootCauses();
    long citedClaims = 0;
    for (AnalysisOutput.Claim claim : claims) {
      validateCitation(claim.evidenceIds(), claim.hypothesis(), availableEvidenceIds, failures);
      if (!claim.evidenceIds().isEmpty()) citedClaims++;
    }
    for (AnalysisOutput.ActionRecommendation action : output.diagnosticActions())
      validateCitation(action.evidenceIds(), action.hypothesis(), availableEvidenceIds, failures);
    for (AnalysisOutput.ActionRecommendation action : output.mitigationActions())
      validateCitation(action.evidenceIds(), action.hypothesis(), availableEvidenceIds, failures);

    boolean contradictorySeverity =
        "SEV1".equals(incidentSeverity)
            && output.riskNotes().stream()
                .anyMatch(note -> note.toLowerCase(Locale.ROOT).contains("no impact"));
    if (contradictorySeverity) failures.add("CONTRADICTORY_SEVERITY");
    double coverage = claims.isEmpty() ? 1 : (double) citedClaims / claims.size();
    QualityReport report =
        new QualityReport(
            failures.isEmpty(), coverage, failures, availableEvidenceIds.size(), claims.size());
    if (!report.valid())
      throw new InvalidAnalysisException(
          "Model output failed deterministic quality checks: " + String.join(", ", failures));
    return report;
  }

  private static void validateCitation(
      List<UUID> ids, boolean hypothesis, Set<UUID> available, List<String> failures) {
    if (ids.isEmpty() && !hypothesis) failures.add("MISSING_CITATION");
    if (!available.containsAll(ids)) failures.add("INVALID_EVIDENCE_REFERENCE");
  }

  public record QualityReport(
      boolean valid,
      double evidenceCoverage,
      List<String> failures,
      int availableEvidenceCount,
      int claimCount) {
    public QualityReport {
      failures = List.copyOf(failures);
    }
  }

  public static final class InvalidAnalysisException extends DomainException {
    public InvalidAnalysisException(String message) {
      super("INVALID_AI_ANALYSIS", message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
  }
}
