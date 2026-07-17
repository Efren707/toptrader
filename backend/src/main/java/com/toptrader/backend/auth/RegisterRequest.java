package com.toptrader.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank String username,
    @NotBlank @Size(min = 8) String password) {

  @Override
  public String toString() {
    return "RegisterRequest[email=" + email + ", username=" + username + ", password=****]";
  }
}
