package com.vaibhav.runbookos.incident;

import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class PayloadSanitizer {
  private static final Set<String> SENSITIVE =
      Set.of(
          "authorization",
          "password",
          "passwd",
          "token",
          "access_token",
          "refresh_token",
          "secret",
          "api_key",
          "cookie",
          "set-cookie");

  public Map<String, Object> sanitize(Map<String, Object> payload) {
    return sanitizeMap(payload);
  }

  private Map<String, Object> sanitizeMap(Map<?, ?> source) {
    Map<String, Object> result = new LinkedHashMap<>();
    source.forEach(
        (key, value) -> {
          String name = String.valueOf(key);
          result.put(
              name,
              SENSITIVE.contains(name.toLowerCase(java.util.Locale.ROOT))
                  ? "[REDACTED]"
                  : sanitizeValue(value));
        });
    return result;
  }

  private Object sanitizeValue(Object value) {
    if (value instanceof Map<?, ?> map) return sanitizeMap(map);
    if (value instanceof List<?> list) return list.stream().map(this::sanitizeValue).toList();
    if (value instanceof String text && text.length() > 8000) return text.substring(0, 8000);
    return value;
  }
}
