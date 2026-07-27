package com.vaibhav.runbookos.config;

import com.vaibhav.runbookos.analysis.AiAnalysisService;
import com.vaibhav.runbookos.incident.*;
import io.micrometer.core.instrument.*;
import java.util.concurrent.atomic.AtomicLong;
import org.aspectj.lang.*;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PlatformMetricsAspect {
  private final Counter incidentsCreated;
  private final Counter incidentsDeduplicated;
  private final Counter workflowFailures;
  private final Counter providerErrors;
  private final Counter aiInputTokens;
  private final Counter aiOutputTokens;
  private final Counter aiEstimatedCost;
  private final Timer workflowDuration;
  private final Timer providerLatency;
  private final AtomicLong activeIncidents = new AtomicLong();

  public PlatformMetricsAspect(MeterRegistry registry, IncidentRepository incidents) {
    incidentsCreated = registry.counter("runbookos.incidents.created");
    incidentsDeduplicated = registry.counter("runbookos.incidents.deduplicated");
    workflowFailures = registry.counter("runbookos.workflows.failures");
    providerErrors = registry.counter("runbookos.providers.errors");
    aiInputTokens = registry.counter("runbookos.ai.tokens", "direction", "input");
    aiOutputTokens = registry.counter("runbookos.ai.tokens", "direction", "output");
    aiEstimatedCost = registry.counter("runbookos.ai.estimated.cost.usd");
    workflowDuration = registry.timer("runbookos.workflows.duration");
    providerLatency = registry.timer("runbookos.providers.latency");
    Gauge.builder("runbookos.incidents.active", activeIncidents, AtomicLong::get)
        .register(registry);
  }

  @AfterReturning(
      pointcut = "execution(* com.vaibhav.runbookos.incident.IncidentService.ingest(..))",
      returning = "incident")
  public void incidentIngested(Incident incident) {
    if (incident.getSignalCount() > 1) incidentsDeduplicated.increment();
    else incidentsCreated.increment();
    activeIncidents.incrementAndGet();
  }

  @AfterReturning(
      pointcut = "execution(* com.vaibhav.runbookos.analysis.AiAnalysisService.analyze(..))",
      returning = "analysis")
  public void analysisCompleted(AiAnalysisService.AnalysisView analysis) {
    aiInputTokens.increment(analysis.inputTokens());
    aiOutputTokens.increment(analysis.outputTokens());
    if (analysis.estimatedCostUsd() != null) {
      aiEstimatedCost.increment(analysis.estimatedCostUsd().doubleValue());
    }
  }

  @Around("execution(* com.vaibhav.runbookos.integration.IntegrationService.validate(..))")
  public Object providerCall(ProceedingJoinPoint call) throws Throwable {
    Timer.Sample sample = Timer.start();
    try {
      return call.proceed();
    } catch (Throwable error) {
      providerErrors.increment();
      throw error;
    } finally {
      sample.stop(providerLatency);
    }
  }

  @Around("execution(* com.vaibhav.runbookos.workflow.OutboxProcessor.process(..))")
  public Object workflowDispatch(ProceedingJoinPoint call) throws Throwable {
    Timer.Sample sample = Timer.start();
    try {
      return call.proceed();
    } catch (Throwable error) {
      workflowFailures.increment();
      throw error;
    } finally {
      sample.stop(workflowDuration);
    }
  }
}
