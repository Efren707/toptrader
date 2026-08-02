package com.toptrader.backend.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
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
}
