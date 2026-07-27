package com.vaibhav.runbookos.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.*;
import java.util.concurrent.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Size, media-type, abuse-rate, and browser hardening at the HTTP trust boundary. */
@Component
public class RequestSecurityFilter extends OncePerRequestFilter {
  static final long MAX_BODY_BYTES = 1_048_576;
  private static final int MAX_SENSITIVE_REQUESTS_PER_MINUTE = 30;
  private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    addHeaders(response);
    if (request.getContentLengthLong() > MAX_BODY_BYTES) {
      reject(response, 413, "REQUEST_TOO_LARGE", "Request body exceeds 1 MiB");
      return;
    }
    if (hasBody(request)
        && (request.getContentLengthLong() > 0 || request.getContentType() != null)
        && !isJson(request.getContentType())) {
      reject(response, 415, "UNSUPPORTED_CONTENT_TYPE", "Use application/json");
      return;
    }
    if (isSensitive(request) && !allow(request.getRemoteAddr() + "|" + request.getRequestURI())) {
      response.setHeader("Retry-After", "60");
      reject(response, 429, "RATE_LIMITED", "Too many requests; retry after one minute");
      return;
    }
    chain.doFilter(request, response);
  }

  private boolean allow(String key) {
    Instant now = Instant.now();
    Window value =
        windows.compute(
            key,
            (ignored, current) -> {
              if (current == null || current.started().plusSeconds(60).isBefore(now)) {
                return new Window(now, 1);
              }
              return new Window(current.started(), current.count() + 1);
            });
    if (windows.size() > 10_000) {
      windows
          .entrySet()
          .removeIf(entry -> entry.getValue().started().plusSeconds(120).isBefore(now));
    }
    return value.count() <= MAX_SENSITIVE_REQUESTS_PER_MINUTE;
  }

  private static boolean hasBody(HttpServletRequest request) {
    return switch (request.getMethod()) {
      case "POST", "PUT", "PATCH" -> true;
      default -> false;
    };
  }

  private static boolean isJson(String contentType) {
    if (contentType == null) return false;
    try {
      return MediaType.APPLICATION_JSON.isCompatibleWith(MediaType.parseMediaType(contentType));
    } catch (Exception ignored) {
      return false;
    }
  }

  private static boolean isSensitive(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/api/auth/") || path.startsWith("/api/webhooks/");
  }

  private static void addHeaders(HttpServletResponse response) {
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("Referrer-Policy", "no-referrer");
    response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
    response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
    response.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
    response.setHeader("Cache-Control", "no-store");
  }

  private static void reject(HttpServletResponse response, int status, String code, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response
        .getWriter()
        .write(
            "{\"code\":\""
                + code
                + "\",\"message\":\""
                + message
                + "\",\"status\":"
                + status
                + ",\"correlationId\":\"unknown\"}");
  }

  private record Window(Instant started, int count) {}
}
