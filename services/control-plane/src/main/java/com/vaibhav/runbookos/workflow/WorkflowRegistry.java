package com.vaibhav.runbookos.workflow;

import com.vaibhav.runbookos.exception.ResourceNotFoundException;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class WorkflowRegistry {
  private final Map<String, WorkflowDefinition> workflows =
      Map.of(
          "incident-triage",
              new WorkflowDefinition(
                  "incident-triage",
                  1,
                  List.of(
                      new StepDefinition("fetch-context", "Fetch incident context"),
                      new StepDefinition("fetch-monitoring", "Fetch monitoring evidence"),
                      new StepDefinition("notify-team", "Notify response team"))),
          "postmortem-draft",
              new WorkflowDefinition(
                  "postmortem-draft",
                  1,
                  List.of(
                      new StepDefinition("fetch-context", "Fetch incident context"),
                      new StepDefinition("generate-postmortem", "Generate postmortem draft"))));

  public WorkflowDefinition require(String key) {
    WorkflowDefinition value = workflows.get(key);
    if (value == null) throw new ResourceNotFoundException("workflow", key);
    return value;
  }

  public record WorkflowDefinition(String key, int version, List<StepDefinition> steps) {
    public List<String> actionIds() {
      return steps.stream().map(StepDefinition::actionId).toList();
    }
  }

  public record StepDefinition(String actionId, String name) {}
}
