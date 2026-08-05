package com.toptrader.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerPasswordResetTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void forgotPassword_withRegisteredEmail_returns204AndCreatesResetToken() throws Exception {
    User user = seedUser("alice@example.com", "alice", "password123");

    mockMvc
        .perform(
            post("/auth/forgot-password")
                .header("X-Forwarded-For", "203.0.113.31")
                .contentType(MediaType.APPLICATION_JSON)
                .content(forgotPasswordRequestBody("alice@example.com")))
        .andExpect(status().isNoContent());

    assertThat(passwordResetTokenRepository.findByUser(user)).hasSize(1);
  }

  @Test
  void forgotPassword_withUnknownEmail_returns204AndCreatesNoToken() throws Exception {
    mockMvc
        .perform(
            post("/auth/forgot-password")
                .header("X-Forwarded-For", "203.0.113.32")
                .contentType(MediaType.APPLICATION_JSON)
                .content(forgotPasswordRequestBody("nobody@example.com")))
        .andExpect(status().isNoContent());

    assertThat(passwordResetTokenRepository.findAll()).isEmpty();
  }

  @Test
  void forgotPassword_withInvalidEmail_returns400WithFieldError() throws Exception {
    mockMvc
        .perform(
            post("/auth/forgot-password")
                .header("X-Forwarded-For", "203.0.113.33")
                .contentType(MediaType.APPLICATION_JSON)
                .content(forgotPasswordRequestBody("not-an-email")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.email").exists());
  }

  @Test
  void resetPassword_withValidToken_returns204UpdatesPasswordAndMarksTokenUsed() throws Exception {
    User user = seedUser("bob@example.com", "bob", "password123");
    String rawToken = "valid-raw-token";
    seedResetToken(user, rawToken, LocalDateTime.now().plusMinutes(10));

    mockMvc
        .perform(
            post("/auth/reset-password")
                .header("X-Forwarded-For", "203.0.113.34")
                .contentType(MediaType.APPLICATION_JSON)
                .content(resetPasswordRequestBody(rawToken, "newPassword123")))
        .andExpect(status().isNoContent());

    User updated = userRepository.findByEmail("bob@example.com").orElseThrow();
    assertThat(passwordEncoder.matches("newPassword123", updated.getPasswordHash())).isTrue();

    PasswordResetToken usedToken =
        passwordResetTokenRepository.findByTokenHash(sha256Hex(rawToken)).orElseThrow();
    assertThat(usedToken.getUsedAt()).isNotNull();
  }

  @Test
  void resetPassword_withUnknownToken_returns400() throws Exception {
    mockMvc
        .perform(
            post("/auth/reset-password")
                .header("X-Forwarded-For", "203.0.113.35")
                .contentType(MediaType.APPLICATION_JSON)
                .content(resetPasswordRequestBody("unknown-token", "newPassword123")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Invalid or expired reset token."));
  }

  @Test
  void resetPassword_withExpiredToken_returns400AndDoesNotUpdatePassword() throws Exception {
    User user = seedUser("carol@example.com", "carol", "password123");
    String rawToken = "expired-token";
    seedResetToken(user, rawToken, LocalDateTime.now().minusMinutes(1));
    String originalHash = user.getPasswordHash();

    mockMvc
        .perform(
            post("/auth/reset-password")
                .header("X-Forwarded-For", "203.0.113.36")
                .contentType(MediaType.APPLICATION_JSON)
                .content(resetPasswordRequestBody(rawToken, "newPassword123")))
        .andExpect(status().isBadRequest());

    User unchanged = userRepository.findByEmail("carol@example.com").orElseThrow();
    assertThat(unchanged.getPasswordHash()).isEqualTo(originalHash);
  }

  @Test
  void resetPassword_withAlreadyUsedToken_returns400AndDoesNotUpdatePassword() throws Exception {
    User user = seedUser("dave@example.com", "dave", "password123");
    String rawToken = "used-token";
    PasswordResetToken resetToken =
        seedResetToken(user, rawToken, LocalDateTime.now().plusMinutes(10));
    resetToken.setUsedAt(LocalDateTime.now().minusMinutes(5));
    passwordResetTokenRepository.save(resetToken);
    String originalHash = user.getPasswordHash();

    mockMvc
        .perform(
            post("/auth/reset-password")
                .header("X-Forwarded-For", "203.0.113.37")
                .contentType(MediaType.APPLICATION_JSON)
                .content(resetPasswordRequestBody(rawToken, "newPassword123")))
        .andExpect(status().isBadRequest());

    User unchanged = userRepository.findByEmail("dave@example.com").orElseThrow();
    assertThat(unchanged.getPasswordHash()).isEqualTo(originalHash);
  }

  @Test
  void resetPassword_withShortNewPassword_returns400WithFieldError() throws Exception {
    User user = seedUser("erin@example.com", "erin", "password123");
    String rawToken = "short-password-token";
    seedResetToken(user, rawToken, LocalDateTime.now().plusMinutes(10));

    mockMvc
        .perform(
            post("/auth/reset-password")
                .header("X-Forwarded-For", "203.0.113.38")
                .contentType(MediaType.APPLICATION_JSON)
                .content(resetPasswordRequestBody(rawToken, "short")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.password").exists());
  }

  private User seedUser(String email, String username, String rawPassword) {
    return userRepository.save(
        new User(email, username, passwordEncoder.encode(rawPassword), new BigDecimal("500.00")));
  }

  private PasswordResetToken seedResetToken(User user, String rawToken, LocalDateTime expiresAt) {
    return passwordResetTokenRepository.save(
        new PasswordResetToken(user, sha256Hex(rawToken), expiresAt));
  }

  private static String sha256Hex(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private String forgotPasswordRequestBody(String email) {
    return """
        {"email":"%s"}
        """
        .formatted(email);
  }

  private String resetPasswordRequestBody(String rawToken, String password) {
    return """
        {"rawToken":"%s","password":"%s"}
        """
        .formatted(rawToken, password);
  }
}
