package com.vaibhav.runbookos.security;

import com.vaibhav.runbookos.config.RunbookOsProperties;
import com.vaibhav.runbookos.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfig {
  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthenticationFilter filter,
      RequestSecurityFilter requestSecurityFilter,
      ObjectMapper objectMapper)
      throws Exception {
    http.csrf(csrf -> csrf.disable())
        .cors(cors -> {})
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/auth/signup", "/api/auth/login", "/api/auth/refresh")
                    .permitAll()
                    .requestMatchers("/api/webhooks/**", "/api/internal/workflows/**")
                    .permitAll()
                    .requestMatchers(
                        "/api/health",
                        "/actuator/health",
                        "/api/v3/api-docs/**",
                        "/api/swagger-ui/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(
                        (request, response, ex) ->
                            writeError(
                                objectMapper,
                                response,
                                401,
                                "UNAUTHORIZED",
                                "Authentication is required"))
                    .accessDeniedHandler(
                        (request, response, ex) ->
                            writeError(
                                objectMapper, response, 403, "ACCESS_DENIED", "Access is denied")))
        .headers(
            headers ->
                headers
                    .frameOptions(frame -> frame.deny())
                    .contentTypeOptions(content -> {})
                    .httpStrictTransportSecurity(
                        hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000)))
        .addFilterBefore(requestSecurityFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(filter, RequestSecurityFilter.class);
    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder(RunbookOsProperties properties) {
    return new BCryptPasswordEncoder(properties.security().passwordHashStrength());
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(RunbookOsProperties properties) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(properties.cors().allowedOrigins());
    configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(
        List.of("Authorization", "Content-Type", "Idempotency-Key", "X-Correlation-ID"));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  private static void writeError(
      ObjectMapper mapper, HttpServletResponse response, int status, String code, String message)
      throws java.io.IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    mapper.writeValue(
        response.getOutputStream(), new ErrorResponse(code, message, status, "unknown"));
  }
}
