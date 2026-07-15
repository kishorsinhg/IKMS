package com.ikms.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestContextFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String requestId = normalize(request.getHeader(ObservabilityHeaders.REQUEST_ID));
    if (requestId == null) {
      requestId = UUID.randomUUID().toString();
    }
    String correlationId = normalize(request.getHeader(ObservabilityHeaders.CORRELATION_ID));
    if (correlationId == null) {
      correlationId = requestId;
    }

    response.setHeader(ObservabilityHeaders.REQUEST_ID, requestId);
    response.setHeader(ObservabilityHeaders.CORRELATION_ID, correlationId);

    try (RequestContextHolder.Scope ignored = RequestContextHolder.openRequest(requestId, correlationId)) {
      filterChain.doFilter(request, response);
    }
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
