package com.vaibhav.runbookos.analysis;

import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class EvidenceGuard {
  private static final Set<String> SECRET_KEYS =
      Set.of(
          "authorization",
          "proxy-authorization",
          "cookie",
          "set-cookie",
          "password",
          "passwd",
          "secret",
          "client_secret",
          "api_key",
          "apikey",
          "access_token",
          "refresh_token",
          "private_token",
          "private_key");
  private static final Pattern CREDENTIAL =
      Pattern.compile(
          "(?i)(bearer\\s+[a-z0-9._~+/=-]{12,}|(?:sk|ghp|glpat|xox[baprs])-?[a-z0-9_-]{12,}|password\\s*[=:]\\s*\\S+)");
  private static final Pattern INJECTION =
      Pattern.compile(
          "(?i)(ignore\\s+(all\\s+)?previous|system\\s+prompt|developer\\s+message|"
              + "override\\s+(your|the)\\s+instructions|act\\s+as\\s+|execute\\s+this\\s+command|"
              + "reveal\\s+(the\\s+)?secret|tool\\s*call)");

  public GuardedEvidence guard(Map<String, Object> content) {
    Counter counter = new Counter();
    Object value = sanitize(content, null, counter);
    @SuppressWarnings("unchecked")
    Map<String, Object> sanitized = (Map<String, Object>) value;
    return new GuardedEvidence(sanitized, counter.redactions, counter.injections);
  }

  private Object sanitize(Object value, String key, Counter counter) {
    if (key != null && SECRET_KEYS.contains(key.toLowerCase(Locale.ROOT))) {
      counter.redactions++;
      return "[REDACTED]";
    }
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> result = new LinkedHashMap<>();
      map.forEach((k, v) -> result.put(String.valueOf(k), sanitize(v, String.valueOf(k), counter)));
      return result;
    }
    if (value instanceof Collection<?> collection) {
      return collection.stream().map(item -> sanitize(item, null, counter)).toList();
    }
    if (value instanceof String text) {
      String bounded = text.substring(0, Math.min(text.length(), 8000));
      if (INJECTION.matcher(bounded).find()) {
        counter.injections++;
        return "[UNTRUSTED_INSTRUCTION_REMOVED]";
      }
      String cleaned = CREDENTIAL.matcher(bounded).replaceAll("[REDACTED]");
      if (!cleaned.equals(bounded)) counter.redactions++;
      return cleaned;
    }
    return value;
  }

  public record GuardedEvidence(
      Map<String, Object> content, int secretRedactions, int rejectedInstructions) {}

  private static final class Counter {
    private int redactions;
    private int injections;
  }
}
