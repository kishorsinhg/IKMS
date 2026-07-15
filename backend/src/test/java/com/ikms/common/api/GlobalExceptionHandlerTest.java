package com.ikms.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.ikms.observability.RequestContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
  }

  @org.junit.jupiter.api.AfterEach
  void tearDown() {
    RequestContextHolder.clear();
  }

  @Test
  void shouldReturnValidationErrorPayload() throws Exception {
    var request = new MockHttpServletRequest("POST", "/test/validate");
    var response = handler.handleMethodArgumentNotValid(
        createValidationException(),
        new HttpHeaders(),
        HttpStatus.BAD_REQUEST,
        new ServletWebRequest(request));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isInstanceOf(ApiError.class);

    var error = (ApiError) response.getBody();
    assertThat(error.code()).isEqualTo("validation_error");
    assertThat(error.path()).isEqualTo("/test/validate");
    assertThat(error.violations()).singleElement().satisfies(violation -> {
      assertThat(violation.field()).isEqualTo("name");
      assertThat(violation.message()).isEqualTo("Name is required.");
    });
  }

  @Test
  void shouldReturnBadRequestForIllegalArgument() {
    var response = withContext(() -> handler.handleIllegalArgument(
        new IllegalArgumentException("Client ID is invalid."),
        requestFor("/test/illegal-argument")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().code()).isEqualTo("invalid_request");
    assertThat(response.getBody().message()).isEqualTo("Client ID is invalid.");
    assertThat(response.getBody().requestId()).isEqualTo("req-1");
    assertThat(response.getBody().correlationId()).isEqualTo("corr-1");
  }

  @Test
  void shouldReturnDeclaredStatusForResponseStatusException() {
    var response = handler.handleResponseStatus(
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Client was not found."),
        requestFor("/test/not-found"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody().code()).isEqualTo("request_failed");
    assertThat(response.getBody().message()).isEqualTo("Client was not found.");
  }

  @Test
  void shouldReturnGenericInternalServerErrorForUnexpectedExceptions() {
    var response = handler.handleUnexpected(
        new IllegalStateException("Boom"),
        requestFor("/test/unexpected"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody().code()).isEqualTo("internal_error");
    assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred.");
  }

  private MethodArgumentNotValidException createValidationException() throws Exception {
    Method method = TestController.class.getDeclaredMethod("validate", TestRequest.class);
    MethodParameter parameter = new MethodParameter(method, 0);
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new TestRequest(""), "testRequest");
    bindingResult.addError(new FieldError("testRequest", "name", "", false, null, null, "Name is required."));
    return new MethodArgumentNotValidException(parameter, bindingResult);
  }

  private HttpServletRequest requestFor(String path) {
    return new MockHttpServletRequest("GET", path);
  }

  private <T> T withContext(java.util.function.Supplier<T> action) {
    try (RequestContextHolder.Scope ignored = RequestContextHolder.openRequest("req-1", "corr-1")) {
      return action.get();
    }
  }

  static class TestController {
    void validate(@Valid TestRequest request) {
    }
  }

  record TestRequest(@NotBlank(message = "Name is required.") String name) {
  }
}
