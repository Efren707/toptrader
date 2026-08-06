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
import java.util.List;
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
class AuthControllerEmailVerificationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private EmailVerificationTokenRepository emailVerificationTokenRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void verifyEmail_withValidToken_returns204MarksUserVerifiedAndTokenUsed() throws Exception {
    User user = seedUser("alice@example.com", "alice");
    String rawToken = "valid-raw-token";
    seedVerificationToken(user, rawToken, LocalDateTime.now().plusMinutes(10));

    mockMvc
        .perform(
            post("/auth/verify-email")
                .header("X-Forwarded-For", "203.0.113.101")
                .contentType(MediaType.APPLICATION_JSON)
                .content(verifyEmailRequestBody(rawToken)))
        .andExpect(status().isNoContent());

    User verified = userRepository.findByEmail("alice@example.com").orElseThrow();
    assertThat(verified.getEmailVerifiedAt()).isNotNull();

    EmailVerificationToken usedToken =
        emailVerificationTokenRepository.findByTokenHash(sha256Hex(rawToken)).orElseThrow();
    assertThat(usedToken.getUsedAt()).isNotNull();
  }

  @Test
  void verifyEmail_withUnknownToken_returns400() throws Exception {
    mockMvc
        .perform(
            post("/auth/verify-email")
                .header("X-Forwarded-For", "203.0.113.102")
                .contentType(MediaType.APPLICATION_JSON)
                .content(verifyEmailRequestBody("unknown-token")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Invalid or expired email verification token."));
  }

  @Test
  void verifyEmail_withExpiredToken_returns400AndDoesNotVerifyUser() throws Exception {
    User user = seedUser("bob@example.com", "bob");
    String rawToken = "expired-token";
    seedVerificationToken(user, rawToken, LocalDateTime.now().minusMinutes(1));

    mockMvc
        .perform(
            post("/auth/verify-email")
                .header("X-Forwarded-For", "203.0.113.103")
                .contentType(MediaType.APPLICATION_JSON)
                .content(verifyEmailRequestBody(rawToken)))
        .andExpect(status().isBadRequest());

    User unchanged = userRepository.findByEmail("bob@example.com").orElseThrow();
    assertThat(unchanged.getEmailVerifiedAt()).isNull();
  }

  @Test
  void verifyEmail_withAlreadyUsedToken_returns400AndDoesNotReVerify() throws Exception {
    User user = seedUser("carol@example.com", "carol");
    String rawToken = "used-token";
    EmailVerificationToken token =
        seedVerificationToken(user, rawToken, LocalDateTime.now().plusMinutes(10));
    token.setUsedAt(LocalDateTime.now().minusMinutes(5));
    emailVerificationTokenRepository.save(token);

    mockMvc
        .perform(
            post("/auth/verify-email")
                .header("X-Forwarded-For", "203.0.113.104")
                .contentType(MediaType.APPLICATION_JSON)
                .content(verifyEmailRequestBody(rawToken)))
        .andExpect(status().isBadRequest());

    User unchanged = userRepository.findByEmail("carol@example.com").orElseThrow();
    assertThat(unchanged.getEmailVerifiedAt()).isNull();
  }

  @Test
  void verifyEmail_withBlankToken_returns400WithFieldError() throws Exception {
    mockMvc
        .perform(
            post("/auth/verify-email")
                .header("X-Forwarded-For", "203.0.113.105")
                .contentType(MediaType.APPLICATION_JSON)
                .content(verifyEmailRequestBody("")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.rawToken").exists());
  }

  @Test
  void resendVerification_withUnverifiedRegisteredEmail_returns204AndCreatesNewToken()
      throws Exception {
    User user = seedUser("dave@example.com", "dave");

    mockMvc
        .perform(
            post("/auth/resend-verification")
                .header("X-Forwarded-For", "203.0.113.106")
                .contentType(MediaType.APPLICATION_JSON)
                .content(resendVerificationRequestBody("dave@example.com")))
        .andExpect(status().isNoContent());

    assertThat(emailVerificationTokenRepository.findByUser(user)).hasSize(1);
  }

  @Test
  void resendVerification_withUnknownEmail_returns204AndCreatesNoToken() throws Exception {
    mockMvc
        .perform(
            post("/auth/resend-verification")
                .header("X-Forwarded-For", "203.0.113.107")
                .contentType(MediaType.APPLICATION_JSON)
                .content(resendVerificationRequestBody("nobody@example.com")))
        .andExpect(status().isNoContent());

    assertThat(emailVerificationTokenRepository.findAll()).isEmpty();
  }

  @Test
  void resendVerification_withAlreadyVerifiedEmail_returns204AndCreatesNoToken() throws Exception {
    User user = seedUser("erin@example.com", "erin");
    user.setEmailVerifiedAt(LocalDateTime.now().minusDays(1));
    userRepository.save(user);

    mockMvc
        .perform(
            post("/auth/resend-verification")
                .header("X-Forwarded-For", "203.0.113.108")
                .contentType(MediaType.APPLICATION_JSON)
                .content(resendVerificationRequestBody("erin@example.com")))
        .andExpect(status().isNoContent());

    assertThat(emailVerificationTokenRepository.findByUser(user)).isEmpty();
  }

  @Test
  void resendVerification_invalidatesPriorOutstandingToken_whenReissuing() throws Exception {
    User user = seedUser("frank@example.com", "frank");
    seedVerificationToken(user, "old-outstanding-token", LocalDateTime.now().plusMinutes(10));

    mockMvc
        .perform(
            post("/auth/resend-verification")
                .header("X-Forwarded-For", "203.0.113.109")
                .contentType(MediaType.APPLICATION_JSON)
                .content(resendVerificationRequestBody("frank@example.com")))
        .andExpect(status().isNoContent());

    List<EmailVerificationToken> tokens = emailVerificationTokenRepository.findByUser(user);
    assertThat(tokens).hasSize(2);
    assertThat(tokens).anyMatch(token -> token.getUsedAt() != null);
    assertThat(tokens).anyMatch(token -> token.getUsedAt() == null);
  }

  @Test
  void resendVerification_withInvalidEmail_returns400WithFieldError() throws Exception {
    mockMvc
        .perform(
            post("/auth/resend-verification")
                .header("X-Forwarded-For", "203.0.113.110")
                .contentType(MediaType.APPLICATION_JSON)
                .content(resendVerificationRequestBody("not-an-email")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.email").exists());
  }

  private User seedUser(String email, String username) {
    return userRepository.save(
        new User(email, username, passwordEncoder.encode("password123"), new BigDecimal("500.00")));
  }

  private EmailVerificationToken seedVerificationToken(
      User user, String rawToken, LocalDateTime expiresAt) {
    return emailVerificationTokenRepository.save(
        new EmailVerificationToken(user, sha256Hex(rawToken), expiresAt));
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

  private String verifyEmailRequestBody(String rawToken) {
    return """
        {"rawToken":"%s"}
        """
        .formatted(rawToken);
  }

  private String resendVerificationRequestBody(String email) {
    return """
        {"email":"%s"}
        """
        .formatted(email);
  }
}
