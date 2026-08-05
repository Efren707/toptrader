package com.toptrader.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank String rawToken, @NotBlank @Size(min = 8) String password) {
  @Override
  public String toString() {
    return "ResetPasswordRequest[token=" + rawToken + ", password=***]";
  }
}
