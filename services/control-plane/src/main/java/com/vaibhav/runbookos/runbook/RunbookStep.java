package com.vaibhav.runbookos.runbook;

import com.vaibhav.runbookos.identity.Role;
import com.vaibhav.runbookos.integration.IntegrationEnvironment;
import jakarta.persistence.*;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "runbook_steps")
public class RunbookStep {
  @Id private UUID id;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Column(name = "runbook_version_id", nullable = false)
  private UUID runbookVersionId;

  @Column(name = "step_key", nullable = false, length = 80)
  private String stepKey;

  @Column(nullable = false, length = 160)
  private String name;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "sequence_number", nullable = false)
  private int sequenceNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "step_type", nullable = false, length = 32)
  private RunbookStepType stepType;

  @Enumerated(EnumType.STRING)
  @Column(name = "risk_classification", nullable = false, length = 24)
  private RiskClassification riskClassification;

  @Enumerated(EnumType.STRING)
  @Column(name = "required_role", nullable = false, length = 24)
  private Role requiredRole;

  @Column(name = "timeout_seconds", nullable = false)
  private int timeoutSeconds;

  @Column(name = "max_retries", nullable = false)
  private int maxRetries;

  @Column(name = "rollback_information", columnDefinition = "text")
  private String rollbackInformation;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "allowed_environments", nullable = false, columnDefinition = "jsonb")
  private List<String> allowedEnvironments;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> configuration;

  protected RunbookStep() {}

  public static RunbookStep create(UUID org, UUID version, StepDraft step) {
    RunbookStep value = new RunbookStep();
    value.id = UUID.randomUUID();
    value.organizationId = org;
    value.runbookVersionId = version;
    value.stepKey = step.stepKey();
    value.name = step.name();
    value.description = step.description();
    value.sequenceNumber = step.sequenceNumber();
    value.stepType = step.stepType();
    value.riskClassification = step.riskClassification();
    value.requiredRole = step.requiredRole();
    value.timeoutSeconds = step.timeoutSeconds();
    value.maxRetries = step.maxRetries();
    value.rollbackInformation = step.rollbackInformation();
    value.allowedEnvironments = step.allowedEnvironments().stream().map(Enum::name).toList();
    value.configuration = Map.copyOf(step.configuration());
    return value;
  }

  public UUID getId() {
    return id;
  }

  public String getStepKey() {
    return stepKey;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public int getSequenceNumber() {
    return sequenceNumber;
  }

  public RunbookStepType getStepType() {
    return stepType;
  }

  public RiskClassification getRiskClassification() {
    return riskClassification;
  }

  public Role getRequiredRole() {
    return requiredRole;
  }

  public int getTimeoutSeconds() {
    return timeoutSeconds;
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public String getRollbackInformation() {
    return rollbackInformation;
  }

  public List<String> getAllowedEnvironments() {
    return List.copyOf(allowedEnvironments);
  }

  public Map<String, Object> getConfiguration() {
    return Map.copyOf(configuration);
  }

  public record StepDraft(
      String stepKey,
      String name,
      String description,
      int sequenceNumber,
      RunbookStepType stepType,
      RiskClassification riskClassification,
      Role requiredRole,
      int timeoutSeconds,
      int maxRetries,
      String rollbackInformation,
      List<IntegrationEnvironment> allowedEnvironments,
      Map<String, Object> configuration) {}
}
