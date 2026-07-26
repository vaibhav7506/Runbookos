package com.vaibhav.runbookos.security;

import com.vaibhav.runbookos.common.TimeProvider;
import com.vaibhav.runbookos.exception.AuthenticationDomainException;
import com.vaibhav.runbookos.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwtService;
  private final TimeProvider timeProvider;
  private final ObjectMapper objectMapper;

  public JwtAuthenticationFilter(
      JwtService jwtService, TimeProvider timeProvider, ObjectMapper objectMapper) {
    this.jwtService = jwtService;
    this.timeProvider = timeProvider;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null && header.startsWith("Bearer ")) {
      try {
        AuthenticatedUser user = jwtService.verify(header.substring(7), timeProvider.now());
        var authorities =
            user.role() == null
                ? List.<SimpleGrantedAuthority>of()
                : List.of(new SimpleGrantedAuthority(user.role().authority()));
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, authorities));
      } catch (AuthenticationDomainException ex) {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
            response.getOutputStream(),
            new ErrorResponse(ex.getCode(), "Access token is invalid or expired", 401, "unknown"));
        return;
      }
    }
    chain.doFilter(request, response);
  }
}
