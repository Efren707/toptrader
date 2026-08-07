package com.toptrader.backend.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void handleAccessDenied_returns403WithProblemDetailBody() {
    ResponseEntity<Object> response =
        handler.handleAccessDenied(new AccessDeniedException("Access is denied"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    ProblemDetail body = (ProblemDetail) response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(body.getDetail()).isEqualTo("Access denied.");
  }

  @Test
  void handleUnexpected_returns500WithGenericDetailAndCorrelationId() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/trades/buy");

    ResponseEntity<Object> response =
        handler.handleUnexpected(new RuntimeException("db connection refused"), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    ProblemDetail body = (ProblemDetail) response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    assertThat(body.getDetail()).isEqualTo("An unexpected error occurred.");
    assertThat(body.getDetail()).doesNotContain("db connection refused");
    assertThat(body.getProperties()).containsKey("correlationId");
    assertThat(body.getProperties().get("correlationId")).isNotNull();
  }
}
