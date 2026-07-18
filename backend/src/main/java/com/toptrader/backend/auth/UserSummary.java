package com.toptrader.backend.auth;

import com.toptrader.backend.user.User;

public record UserSummary(Long id, String email, String username) {

  public static UserSummary from(User user) {
    return new UserSummary(user.getId(), user.getEmail(), user.getUsername());
  }
}
