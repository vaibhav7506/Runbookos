package com.vaibhav.runbookos.analysis;

import com.vaibhav.runbookos.incident.Incident;
import com.vaibhav.runbookos.incident.IncidentEvidence;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DemoAiModelClient {
  public AnalysisOutput analyze(Incident incident, List<IncidentEvidence> evidence) {
    var ids = evidence.stream().map(IncidentEvidence::getId).toList();
    var primary = ids.isEmpty() ? List.<java.util.UUID>of() : List.of(ids.get(0));
    boolean hypothesis = primary.isEmpty();
    return new AnalysisOutput(
        "A deterministic review indicates a likely regression affecting "
            + incident.getAffectedService()
            + ". Human validation is required before remediation.",
        List.of(
            new AnalysisOutput.Claim(
                "The incident began near the most recent observed service change.",
                primary,
                hypothesis)),
        evidence.isEmpty() ? 0.45 : 0.82,
        List.of(incident.getAffectedService()),
        evidence.isEmpty()
            ? List.of("Deployment and runtime evidence has not been collected.")
            : List.of("Confirm impact from an independent service metric."),
        List.of(
            new AnalysisOutput.ActionRecommendation(
                "Compare deployment timing with error-rate onset",
                "This can validate or reject the leading explanation.",
                primary,
                hypothesis)),
        List.of(
            new AnalysisOutput.ActionRecommendation(
                "Prepare a rollback plan for human review",
                "A reversible mitigation may reduce impact if the deployment is confirmed.",
                primary,
                hypothesis)),
        List.of("This analysis is advisory and cannot authorize or execute remediation."));
  }
}
