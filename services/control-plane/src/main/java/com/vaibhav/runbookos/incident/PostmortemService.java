package com.vaibhav.runbookos.incident;

import com.vaibhav.runbookos.common.TimeProvider;
import com.vaibhav.runbookos.exception.ConflictException;
import com.vaibhav.runbookos.identity.Role;
import com.vaibhav.runbookos.organization.TenantAccessService;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostmortemService {
  private final IncidentPostmortemRepository postmortems;
  private final IncidentService incidents;
  private final TenantAccessService access;
  private final TimeProvider time;

  public PostmortemService(
      IncidentPostmortemRepository postmortems,
      IncidentService incidents,
      TenantAccessService access,
      TimeProvider time) {
    this.postmortems = postmortems;
    this.incidents = incidents;
    this.access = access;
    this.time = time;
  }

  @Transactional
  public PostmortemView generate(UUID org, UUID incidentId, UUID actor) {
    access.require(org, actor, Role.OWNER, Role.ADMIN, Role.RESPONDER);
    Incident incident = incidents.require(org, incidentId);
    if (incident.getStatus() != IncidentStatus.RESOLVED
        && incident.getStatus() != IncidentStatus.CLOSED) {
      throw new ConflictException(
          "INCIDENT_NOT_RESOLVED", "Resolve the incident before generating its postmortem");
    }
    return postmortems
        .findByOrganizationIdAndIncidentId(org, incidentId)
        .map(PostmortemView::from)
        .orElseGet(
            () ->
                PostmortemView.from(
                    postmortems.save(
                        IncidentPostmortem.create(org, incident, actor, time.nowTruncated()))));
  }

  @Transactional(readOnly = true)
  public Optional<PostmortemView> find(UUID org, UUID incidentId, UUID actor) {
    access.require(org, actor);
    incidents.require(org, incidentId);
    return postmortems.findByOrganizationIdAndIncidentId(org, incidentId).map(PostmortemView::from);
  }

  public record PostmortemView(
      UUID id,
      UUID incidentId,
      String title,
      String summary,
      String impact,
      String rootCause,
      String resolution,
      List<String> followUpActions,
      Instant generatedAt) {
    static PostmortemView from(IncidentPostmortem value) {
      return new PostmortemView(
          value.getId(),
          value.getIncidentId(),
          value.getTitle(),
          value.getSummary(),
          value.getImpact(),
          value.getRootCause(),
          value.getResolution(),
          value.getFollowUpActions(),
          value.getGeneratedAt());
    }
  }
}
