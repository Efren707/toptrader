package com.toptrader.backend.web;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Adds per-field validation detail to the RFC 7807 body (ADR 0012's single error shape). */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(status, "Validation failed for one or more fields.");

    Map<String, String> fieldErrors = new LinkedHashMap<>();
    for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
      fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
    }
    problemDetail.setProperty("errors", fieldErrors);

    return ResponseEntity.status(status).headers(headers).body(problemDetail);
  }

  /**
   * Handles constraint violations on {@code @PathVariable}/{@code @RequestParam} (enforced via
   * class-level {@code @Validated}), which is a separate mechanism from {@code @Valid @RequestBody}
   * above and isn't covered by {@link #handleMethodArgumentNotValid}.
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Validation failed for one or more fields.");

    Map<String, String> fieldErrors = new LinkedHashMap<>();
    for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
      String path = violation.getPropertyPath().toString();
      String field = path.substring(path.lastIndexOf('.') + 1);
      fieldErrors.put(field, violation.getMessage());
    }
    problemDetail.setProperty("errors", fieldErrors);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
  }

  /** Maps a denied {@code @PreAuthorize} check (ADR 0035) to the app's RFC 7807 error shape. */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied.");
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
  }
}
