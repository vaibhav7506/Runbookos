package com.vaibhav.runbookos.analysis;

import com.vaibhav.runbookos.analysis.AnalysisPrompt.EvidenceInput;
import com.vaibhav.runbookos.audit.*;
import com.vaibhav.runbookos.common.TimeProvider;
import com.vaibhav.runbookos.exception.*;
import com.vaibhav.runbookos.identity.Role;
import com.vaibhav.runbookos.incident.*;
import com.vaibhav.runbookos.organization.*;
import com.vaibhav.runbookos.security.crypto.SecretCipher;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class AiAnalysisService {
  private final IncidentService incidents;
  private final IncidentEvidenceRepository evidenceRepository;
  private final IncidentAnalysisRepository analyses;
  private final AiProviderConfigRepository configs;
  private final AiProviderFailureRepository failures;
  private final OrganizationRepository organizations;
  private final TenantAccessService access;
  private final EvidenceGuard evidenceGuard;
  private final AnalysisQualityValidator qualityValidator;
  private final DemoAiModelClient demoClient;
  private final List<AiModelClient> modelClients;
  private final SecretCipher cipher;
  private final ObjectMapper mapper;
  private final AuditService audit;
  private final TimeProvider time;

  public AiAnalysisService(
      IncidentService incidents,
      IncidentEvidenceRepository evidenceRepository,
      IncidentAnalysisRepository analyses,
      AiProviderConfigRepository configs,
      AiProviderFailureRepository failures,
      OrganizationRepository organizations,
      TenantAccessService access,
      EvidenceGuard evidenceGuard,
      AnalysisQualityValidator qualityValidator,
      DemoAiModelClient demoClient,
      List<AiModelClient> modelClients,
      SecretCipher cipher,
      ObjectMapper mapper,
      AuditService audit,
      TimeProvider time) {
    this.incidents = incidents;
    this.evidenceRepository = evidenceRepository;
    this.analyses = analyses;
    this.configs = configs;
    this.failures = failures;
    this.organizations = organizations;
    this.access = access;
    this.evidenceGuard = evidenceGuard;
    this.qualityValidator = qualityValidator;
    this.demoClient = demoClient;
    this.modelClients = List.copyOf(modelClients);
    this.cipher = cipher;
    this.mapper = mapper;
    this.audit = audit;
    this.time = time;
  }

  @Transactional
  public AnalysisView analyze(UUID org, UUID incidentId, UUID actor) {
    access.require(org, actor, Role.OWNER, Role.ADMIN, Role.RESPONDER);
    Incident incident = incidents.require(org, incidentId);
    Organization organization =
        organizations
            .findById(org)
            .orElseThrow(() -> new ResourceNotFoundException("organization", org));
    List<IncidentEvidence> evidence =
        evidenceRepository.findByIncidentIdOrderByCollectedAtAsc(incidentId);
    List<EvidenceInput> guarded = new ArrayList<>();
    int redactions = 0;
    int rejectedInstructions = 0;
    for (IncidentEvidence item : evidence) {
      EvidenceGuard.GuardedEvidence result = evidenceGuard.guard(item.getContent());
      redactions += result.secretRedactions();
      rejectedInstructions += result.rejectedInstructions();
      guarded.add(
          new EvidenceInput(item.getId(), item.getKind(), item.getTitle(), result.content()));
    }

    long started = System.nanoTime();
    AnalysisOutput output = null;
    AiProvider usedProvider = AiProvider.DEMO;
    String usedModel = "deterministic-v1";
    int inputTokens = approximateTokens(guarded.toString());
    int outputTokens = 0;
    String fallbackReason = null;

    if (!organization.isDemoMode()) {
      List<String> providerErrors = new ArrayList<>();
      for (AiProviderConfig config :
          configs.findByOrganizationIdAndEnabledTrueOrderByPriorityAsc(org)) {
        AiProviderFailure failure =
            failures
                .findByOrganizationIdAndProvider(org, config.getProvider())
                .orElseGet(
                    () ->
                        failures.save(
                            AiProviderFailure.create(
                                org, config.getProvider(), time.nowTruncated())));
        if (failure.isOpen(time.now())) {
          providerErrors.add(config.getProvider() + ": circuit open");
          continue;
        }
        String prompt =
            AnalysisPrompt.render(
                incident.getTitle(),
                incident.getSeverity().name(),
                incident.getAffectedService(),
                guarded,
                config.getOutputTokenBudget());
        if (approximateTokens(prompt) > config.getInputTokenBudget()) {
          providerErrors.add(config.getProvider() + ": token budget exceeded");
          continue;
        }
        AiModelClient client =
            modelClients.stream()
                .filter(candidate -> candidate.supports(config.getProvider()))
                .findFirst()
                .orElse(null);
        if (client == null) continue;
        for (int attempt = 0; attempt <= config.getMaxRetries(); attempt++) {
          try {
            AiModelClient.ModelResult result = client.analyze(config, prompt);
            BigDecimal estimated = estimateCost(result.inputTokens(), result.outputTokens());
            if (estimated.compareTo(config.getCostBudgetUsd()) > 0)
              throw new IllegalStateException("cost budget exceeded");
            output = result.output();
            inputTokens = result.inputTokens();
            outputTokens = result.outputTokens();
            usedProvider = config.getProvider();
            usedModel = config.getModel();
            failure.succeeded(time.nowTruncated());
            break;
          } catch (Exception ex) {
            failure.failed(safeFailure(ex), time.nowTruncated());
            if (attempt == config.getMaxRetries())
              providerErrors.add(config.getProvider() + ": " + safeFailure(ex));
          }
        }
        if (output != null) break;
      }
      if (output == null && !providerErrors.isEmpty())
        fallbackReason = String.join("; ", providerErrors);
    }

    if (output == null) {
      output = demoClient.analyze(incident, evidence);
      outputTokens = approximateTokens(output.toString());
      if (!organization.isDemoMode() && fallbackReason == null)
        fallbackReason = "No enabled external provider; deterministic analyzer used";
    }

    Set<UUID> evidenceIds = new HashSet<>();
    evidence.forEach(item -> evidenceIds.add(item.getId()));
    AnalysisQualityValidator.QualityReport quality;
    try {
      quality = qualityValidator.validate(output, evidenceIds, incident.getSeverity().name());
    } catch (AnalysisQualityValidator.InvalidAnalysisException ex) {
      audit.recordDenied(
          AuditActions.AI_ANALYSIS_REJECTED, "incident_analysis", org, actor, ex.getMessage());
      throw ex;
    }
    long latencyMs = Duration.ofNanos(System.nanoTime() - started).toMillis();
    Map<String, Object> outputMap =
        mapper.convertValue(output, new TypeReference<Map<String, Object>>() {});
    Map<String, Object> qualityMap =
        mapper.convertValue(quality, new TypeReference<Map<String, Object>>() {});
    qualityMap = new LinkedHashMap<>(qualityMap);
    qualityMap.put("secretRedactions", redactions);
    qualityMap.put("rejectedInstructions", rejectedInstructions);
    IncidentAnalysis saved =
        analyses.save(
            IncidentAnalysis.completed(
                org,
                incidentId,
                usedProvider,
                usedModel,
                outputMap,
                qualityMap,
                latencyMs,
                inputTokens,
                outputTokens,
                estimateCost(inputTokens, outputTokens),
                fallbackReason,
                actor,
                time.nowTruncated()));
    audit.recordSuccess(
        AuditActions.AI_ANALYSIS_COMPLETED, "incident_analysis", org, actor, saved.getId());
    return AnalysisView.from(saved);
  }

  @Transactional(readOnly = true)
  public List<AnalysisView> list(UUID org, UUID incidentId, UUID actor) {
    access.require(org, actor);
    incidents.require(org, incidentId);
    return analyses.findByOrganizationIdAndIncidentIdOrderByCreatedAtDesc(org, incidentId).stream()
        .map(AnalysisView::from)
        .toList();
  }

  @Transactional
  public ProviderConfigView configure(UUID org, ConfigureProviderRequest request, UUID actor) {
    access.require(org, actor, Role.OWNER, Role.ADMIN);
    if (request.provider() == AiProvider.DEMO)
      throw new ConflictException("INVALID_PROVIDER", "Demo provider does not accept a key");
    validateProviderUrl(request.provider(), request.baseUrl());
    SecretCipher.EncryptedValue encrypted = cipher.encrypt(request.apiKey());
    AiProviderConfig saved =
        configs.save(
            AiProviderConfig.create(
                org,
                request.provider(),
                request.model(),
                request.baseUrl(),
                encrypted.ciphertext(),
                encrypted.nonce(),
                encrypted.keyId(),
                encrypted.fingerprint(),
                request.priority(),
                request.timeoutSeconds(),
                request.maxRetries(),
                request.inputTokenBudget(),
                request.outputTokenBudget(),
                request.costBudgetUsd(),
                actor,
                time.nowTruncated()));
    audit.recordSuccess(
        AuditActions.AI_PROVIDER_CONFIGURED, "ai_provider_config", org, actor, saved.getId());
    return ProviderConfigView.from(saved);
  }

  @Transactional(readOnly = true)
  public List<ProviderConfigView> providerConfigs(UUID org, UUID actor) {
    access.require(org, actor, Role.OWNER, Role.ADMIN);
    return configs.findByOrganizationIdOrderByPriorityAsc(org).stream()
        .map(ProviderConfigView::from)
        .toList();
  }

  @Transactional
  public void disableProvider(UUID org, UUID id, UUID actor) {
    access.require(org, actor, Role.OWNER, Role.ADMIN);
    AiProviderConfig config =
        configs
            .findByIdAndOrganizationId(id, org)
            .orElseThrow(() -> new ResourceNotFoundException("AI provider", id));
    config.disable(time.nowTruncated());
    audit.recordSuccess(
        AuditActions.AI_PROVIDER_DISABLED, "ai_provider_config", org, actor, config.getId());
  }

  private static int approximateTokens(String text) {
    return Math.max(1, text.length() / 4);
  }

  private static void validateProviderUrl(AiProvider provider, String baseUrl) {
    if (baseUrl == null || baseUrl.isBlank()) {
      if (provider == AiProvider.OPENAI_COMPATIBLE)
        throw new ConflictException(
            "PROVIDER_URL_REQUIRED", "A compatible provider requires an HTTPS base URL");
      return;
    }
    if (provider != AiProvider.OPENAI_COMPATIBLE)
      throw new ConflictException(
          "PROVIDER_URL_NOT_ALLOWED", "Official providers use their fixed API endpoint");
    try {
      java.net.URI uri = java.net.URI.create(baseUrl);
      String host = uri.getHost();
      if (!"https".equalsIgnoreCase(uri.getScheme())
          || host == null
          || host.equalsIgnoreCase("localhost")
          || host.matches("(?i)^(127\\.|10\\.|192\\.168\\.|169\\.254\\.|0\\.).*")
          || host.matches("^172\\.(1[6-9]|2\\d|3[01])\\..*")
          || host.equals("::1")) throw new IllegalArgumentException();
    } catch (IllegalArgumentException ex) {
      throw new ConflictException(
          "UNSAFE_PROVIDER_URL", "Compatible provider URLs must be public HTTPS endpoints");
    }
  }

  private static BigDecimal estimateCost(int inputTokens, int outputTokens) {
    return BigDecimal.valueOf(inputTokens * 0.000002 + outputTokens * 0.000006)
        .setScale(6, java.math.RoundingMode.HALF_UP);
  }

  private static String safeFailure(Exception ex) {
    String name = ex.getClass().getSimpleName();
    return name.length() > 120 ? name.substring(0, 120) : name;
  }

  public record ConfigureProviderRequest(
      AiProvider provider,
      String model,
      String baseUrl,
      String apiKey,
      int priority,
      int timeoutSeconds,
      int maxRetries,
      int inputTokenBudget,
      int outputTokenBudget,
      BigDecimal costBudgetUsd) {}

  public record ProviderConfigView(
      UUID id,
      AiProvider provider,
      String model,
      String baseUrl,
      String keyFingerprint,
      int priority,
      boolean enabled,
      int timeoutSeconds,
      int maxRetries,
      int inputTokenBudget,
      int outputTokenBudget,
      BigDecimal costBudgetUsd) {
    static ProviderConfigView from(AiProviderConfig value) {
      return new ProviderConfigView(
          value.getId(),
          value.getProvider(),
          value.getModel(),
          value.getBaseUrl(),
          value.getKeyFingerprint(),
          value.getPriority(),
          value.isEnabled(),
          value.getTimeoutSeconds(),
          value.getMaxRetries(),
          value.getInputTokenBudget(),
          value.getOutputTokenBudget(),
          value.getCostBudgetUsd());
    }
  }

  public record AnalysisView(
      UUID id,
      UUID incidentId,
      AiProvider provider,
      String model,
      int promptVersion,
      Map<String, Object> output,
      Map<String, Object> quality,
      long latencyMs,
      int inputTokens,
      int outputTokens,
      BigDecimal estimatedCostUsd,
      String fallbackReason,
      java.time.Instant createdAt) {
    static AnalysisView from(IncidentAnalysis value) {
      return new AnalysisView(
          value.getId(),
          value.getIncidentId(),
          value.getProvider(),
          value.getModel(),
          value.getPromptVersion(),
          value.getOutput(),
          value.getQuality(),
          value.getLatencyMs(),
          value.getInputTokens(),
          value.getOutputTokens(),
          value.getEstimatedCostUsd(),
          value.getFallbackReason(),
          value.getCreatedAt());
    }
  }
}
