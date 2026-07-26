package com.vaibhav.runbookos.incident;

import com.vaibhav.runbookos.exception.ConflictException;
import java.util.Map;
import java.util.Set;

public final class IncidentStateMachine {
  private static final Map<IncidentStatus, Set<IncidentStatus>> ALLOWED =
      Map.of(
          IncidentStatus.DETECTED,
              Set.of(
                  IncidentStatus.TRIAGED, IncidentStatus.INVESTIGATING, IncidentStatus.CANCELLED),
          IncidentStatus.TRIAGED, Set.of(IncidentStatus.INVESTIGATING, IncidentStatus.CANCELLED),
          IncidentStatus.INVESTIGATING,
              Set.of(
                  IncidentStatus.AWAITING_APPROVAL,
                  IncidentStatus.MITIGATING,
                  IncidentStatus.MONITORING,
                  IncidentStatus.RESOLVED,
                  IncidentStatus.CANCELLED),
          IncidentStatus.AWAITING_APPROVAL,
              Set.of(
                  IncidentStatus.INVESTIGATING,
                  IncidentStatus.MITIGATING,
                  IncidentStatus.CANCELLED),
          IncidentStatus.MITIGATING,
              Set.of(
                  IncidentStatus.MONITORING, IncidentStatus.INVESTIGATING, IncidentStatus.RESOLVED),
          IncidentStatus.MONITORING, Set.of(IncidentStatus.INVESTIGATING, IncidentStatus.RESOLVED),
          IncidentStatus.RESOLVED, Set.of(IncidentStatus.CLOSED, IncidentStatus.INVESTIGATING),
          IncidentStatus.CLOSED, Set.of(),
          IncidentStatus.CANCELLED, Set.of());

  private IncidentStateMachine() {}

  public static void requireAllowed(IncidentStatus from, IncidentStatus to) {
    if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
      throw new ConflictException(
          "INVALID_INCIDENT_TRANSITION", "Incident cannot transition from " + from + " to " + to);
    }
  }
}
