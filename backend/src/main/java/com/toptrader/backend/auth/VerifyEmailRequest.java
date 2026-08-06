package com.toptrader.backend.auth;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(@NotBlank String rawToken) {
  @Override
  public String toString() {
    return "EmailVerificationRequest[token=" + rawToken + "]";
  }
}
