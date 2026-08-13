package com.toptrader.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @Email String email, String username, @Size(min = 8) String password, String avatarKey) {

  @Override
  public String toString() {
    return "UpdateProfileRequest[email="
        + email
        + ", username="
        + username
        + ", avatarKey="
        + avatarKey
        + ", password=****]";
  }
}
