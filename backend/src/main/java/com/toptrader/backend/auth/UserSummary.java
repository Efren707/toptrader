package com.toptrader.backend.auth;

import com.toptrader.backend.user.User;
import java.math.BigDecimal;

public record UserSummary(Long id, String email, String username, BigDecimal cashBalance) {

  public static UserSummary from(User user) {
    return new UserSummary(
        user.getId(), user.getEmail(), user.getUsername(), user.getCashBalance());
  }
}
