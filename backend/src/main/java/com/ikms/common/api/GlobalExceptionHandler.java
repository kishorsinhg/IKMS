package com.ikms.common.api;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import com.ikms.observability.RequestContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    var violations = ex.getBindingResult().getFieldErrors().stream()
        .map(this::toViolation)
        .toList();

    return buildObjectResponse(
        HttpStatus.BAD_REQUEST,
        "validation_error",
        "Request validation failed.",
        resolvePath(request),
        violations);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> handleIllegalArgument(
      IllegalArgumentException ex,
      HttpServletRequest request) {
    return buildApiErrorResponse(
        HttpStatus.BAD_REQUEST,
        "invalid_request",
        ex.getMessage(),
        request.getRequestURI(),
        List.of());
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiError> handleResponseStatus(
      ResponseStatusException ex,
      HttpServletRequest request) {
    var message = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
    return buildApiErrorResponse(
        HttpStatus.valueOf(ex.getStatusCode().value()),
        "request_failed",
        message,
        request.getRequestURI(),
        List.of());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(
      Exception ex,
      HttpServletRequest request) {
    return buildApiErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "internal_error",
        "An unexpected error occurred.",
        request.getRequestURI(),
        List.of());
  }

  private ApiError.FieldViolation toViolation(FieldError fieldError) {
    return new ApiError.FieldViolation(
        fieldError.getField(),
        fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value.");
  }

  private String resolvePath(WebRequest request) {
    if (request instanceof ServletWebRequest servletWebRequest) {
      return servletWebRequest.getRequest().getRequestURI();
    }
    return "";
  }

  private ResponseEntity<Object> buildObjectResponse(
      HttpStatus status,
      String code,
      String message,
      String path,
      List<ApiError.FieldViolation> violations) {
    return ResponseEntity.status(status).body(buildBody(status, code, message, path, violations));
  }

  private ResponseEntity<ApiError> buildApiErrorResponse(
      HttpStatus status,
      String code,
      String message,
      String path,
      List<ApiError.FieldViolation> violations) {
    return ResponseEntity.status(status).body(buildBody(status, code, message, path, violations));
  }

  private ApiError buildBody(
      HttpStatus status,
      String code,
      String message,
      String path,
      List<ApiError.FieldViolation> violations) {
    return new ApiError(
        Instant.now(),
        status.value(),
        status.getReasonPhrase(),
        code,
        message,
        path,
        RequestContextHolder.requestId(),
        RequestContextHolder.correlationId(),
        violations);
  }
}
