package com.ikms.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestContextFilterTest {

  private final RequestContextFilter filter = new RequestContextFilter();

  @AfterEach
  void tearDown() {
    RequestContextHolder.clear();
  }

  @Test
  void shouldGenerateRequestAndCorrelationIdentifiers() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getHeader(ObservabilityHeaders.REQUEST_ID)).isNotBlank();
    assertThat(response.getHeader(ObservabilityHeaders.CORRELATION_ID)).isEqualTo(response.getHeader(ObservabilityHeaders.REQUEST_ID));
    assertThat(RequestContextHolder.requestId()).isNull();
  }

  @Test
  void shouldPreserveInboundCorrelationHeaders() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    request.addHeader(ObservabilityHeaders.REQUEST_ID, "req-123");
    request.addHeader(ObservabilityHeaders.CORRELATION_ID, "corr-456");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, recordingChain());

    assertThat(response.getHeader(ObservabilityHeaders.REQUEST_ID)).isEqualTo("req-123");
    assertThat(response.getHeader(ObservabilityHeaders.CORRELATION_ID)).isEqualTo("corr-456");
  }

  private MockFilterChain recordingChain() {
    return new MockFilterChain() {
      @Override
      public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response)
          throws IOException, jakarta.servlet.ServletException {
        assertThat(RequestContextHolder.requestId()).isEqualTo("req-123");
        assertThat(RequestContextHolder.correlationId()).isEqualTo("corr-456");
      }
    };
  }
}
