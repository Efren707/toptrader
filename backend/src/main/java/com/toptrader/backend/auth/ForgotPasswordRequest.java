package com.toptrader.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(@NotBlank @Email String email) {
  @Override
  public String toString() {
    return "ForgotPasswordRequest[email=" + email + "]";
  }
}
