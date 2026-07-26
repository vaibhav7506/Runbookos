package com.vaibhav.runbookos.analysis;

public interface AiModelClient {
  boolean supports(AiProvider provider);

  ModelResult analyze(AiProviderConfig config, String prompt);

  record ModelResult(AnalysisOutput output, int inputTokens, int outputTokens) {}
}
