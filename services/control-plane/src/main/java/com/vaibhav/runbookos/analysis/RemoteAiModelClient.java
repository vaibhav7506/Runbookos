package com.vaibhav.runbookos.analysis;

import com.vaibhav.runbookos.security.crypto.SecretCipher;
import java.time.Duration;
import java.util.*;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class RemoteAiModelClient implements AiModelClient {
  private final SecretCipher cipher;
  private final ObjectMapper mapper;

  public RemoteAiModelClient(SecretCipher cipher, ObjectMapper mapper) {
    this.cipher = cipher;
    this.mapper = mapper;
  }

  @Override
  public boolean supports(AiProvider provider) {
    return provider != AiProvider.DEMO;
  }

  @Override
  public ModelResult analyze(AiProviderConfig config, String prompt) {
    String apiKey = cipher.decrypt(config.getEncryptedApiKey(), config.getKeyNonce());
    var requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()));
    requestFactory.setReadTimeout(Duration.ofSeconds(config.getTimeoutSeconds()));
    RestClient client = RestClient.builder().requestFactory(requestFactory).build();
    String response =
        switch (config.getProvider()) {
          case OPENAI, GROQ, OPENAI_COMPATIBLE -> openAiCompatible(client, config, apiKey, prompt);
          case ANTHROPIC -> anthropic(client, config, apiKey, prompt);
          case GEMINI -> gemini(client, config, apiKey, prompt);
          case DEMO -> throw new IllegalArgumentException("Demo provider is local");
        };
    return decode(config.getProvider(), response, prompt);
  }

  private String openAiCompatible(
      RestClient client, AiProviderConfig config, String key, String prompt) {
    String base =
        switch (config.getProvider()) {
          case GROQ -> "https://api.groq.com/openai";
          case OPENAI -> "https://api.openai.com";
          case OPENAI_COMPATIBLE -> configured(config, "");
          default -> throw new IllegalArgumentException("Unsupported compatible provider");
        };
    return client
        .post()
        .uri(base + "/v1/chat/completions")
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + key)
        .body(
            Map.of(
                "model", config.getModel(),
                "response_format", Map.of("type", "json_object"),
                "max_tokens", config.getOutputTokenBudget(),
                "messages", List.of(Map.of("role", "user", "content", prompt))))
        .retrieve()
        .body(String.class);
  }

  private String anthropic(RestClient client, AiProviderConfig config, String key, String prompt) {
    return client
        .post()
        .uri("https://api.anthropic.com/v1/messages")
        .contentType(MediaType.APPLICATION_JSON)
        .header("x-api-key", key)
        .header("anthropic-version", "2023-06-01")
        .body(
            Map.of(
                "model", config.getModel(),
                "max_tokens", config.getOutputTokenBudget(),
                "messages", List.of(Map.of("role", "user", "content", prompt))))
        .retrieve()
        .body(String.class);
  }

  private String gemini(RestClient client, AiProviderConfig config, String key, String prompt) {
    return client
        .post()
        .uri(
            "https://generativelanguage.googleapis.com"
                + "/v1beta/models/"
                + config.getModel()
                + ":generateContent")
        .contentType(MediaType.APPLICATION_JSON)
        .header("x-goog-api-key", key)
        .body(
            Map.of(
                "generationConfig",
                Map.of(
                    "responseMimeType",
                    "application/json",
                    "maxOutputTokens",
                    config.getOutputTokenBudget()),
                "contents",
                List.of(Map.of("parts", List.of(Map.of("text", prompt))))))
        .retrieve()
        .body(String.class);
  }

  private ModelResult decode(AiProvider provider, String response, String prompt) {
    try {
      Map<String, Object> body =
          mapper.readValue(response, new TypeReference<Map<String, Object>>() {});
      String content =
          switch (provider) {
            case OPENAI, GROQ, OPENAI_COMPATIBLE ->
                stringAt(body, "choices", 0, "message", "content");
            case ANTHROPIC -> stringAt(body, "content", 0, "text");
            case GEMINI -> stringAt(body, "candidates", 0, "content", "parts", 0, "text");
            case DEMO -> throw new IllegalArgumentException("Demo provider is local");
          };
      AnalysisOutput output = mapper.readValue(content, AnalysisOutput.class);
      int input = numberAt(body, approximateTokens(prompt), "usage", "prompt_tokens");
      int outputTokens = numberAt(body, approximateTokens(content), "usage", "completion_tokens");
      return new ModelResult(output, input, outputTokens);
    } catch (Exception e) {
      throw new IllegalStateException("Provider returned invalid structured output", e);
    }
  }

  @SuppressWarnings("unchecked")
  private static String stringAt(Map<String, Object> root, Object... path) {
    Object value = root;
    for (Object item : path) {
      value =
          item instanceof Integer index
              ? ((List<Object>) value).get(index)
              : ((Map<String, Object>) value).get(item.toString());
    }
    if (!(value instanceof String text) || text.isBlank())
      throw new IllegalStateException("Provider response did not contain JSON content");
    return text;
  }

  @SuppressWarnings("unchecked")
  private static int numberAt(Map<String, Object> root, int fallback, String... path) {
    Object value = root;
    for (String item : path) {
      if (!(value instanceof Map<?, ?> map)) return fallback;
      value = ((Map<String, Object>) map).get(item);
    }
    return value instanceof Number number ? number.intValue() : fallback;
  }

  private static int approximateTokens(String value) {
    return Math.max(1, value.length() / 4);
  }

  private static String configured(AiProviderConfig config, String fallback) {
    return config.getBaseUrl() == null || config.getBaseUrl().isBlank()
        ? fallback
        : config.getBaseUrl().replaceAll("/+$", "");
  }
}
