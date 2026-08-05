package com.toptrader.backend.auth;

import com.toptrader.backend.email.EmailSender;
import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PasswordResetService {

  private static final int RESET_TOKEN_TTL_MINUTES = 30;

  private final UserRepository userRepository;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final Optional<EmailSender> emailSender;

  @Value("${toptrader.frontend-origin:http://localhost:4200}")
  private String frontendOrigin;

  public PasswordResetService(
      UserRepository userRepository,
      PasswordResetTokenRepository passwordResetTokenRepository,
      PasswordEncoder passwordEncoder,
      Optional<EmailSender> emailSender) {
    this.userRepository = userRepository;
    this.passwordResetTokenRepository = passwordResetTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.emailSender = emailSender;
  }

  public void resetRequest(String email) {
    if (emailSender.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Password reset is temporarily unavailable.");
    }

    SecureRandom secureRandom = new SecureRandom();
    byte[] tokenBytes = new byte[32];
    secureRandom.nextBytes(tokenBytes);
    String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    String tokenHash = hashToken(rawToken);

    Optional<User> user = userRepository.findByEmail(email);
    if (user.isPresent()) {
      LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(RESET_TOKEN_TTL_MINUTES);
      PasswordResetToken passwordResetToken =
          new PasswordResetToken(user.get(), tokenHash, expiresAt);
      passwordResetTokenRepository.save(passwordResetToken);

      String resetLink = frontendOrigin + "/reset-password?token=" + rawToken;
      emailSender
          .get()
          .send(
              user.get().getEmail(),
              "Reset your TopTrader password",
              "Use this link to reset your password: " + resetLink);
    }
  }

  public void resetPassword(String rawToken, String newPassword) {
    String tokenHash = hashToken(rawToken);
    Optional<PasswordResetToken> maybeToken =
        passwordResetTokenRepository.findByTokenHash(tokenHash);

    boolean valid =
        maybeToken.isPresent()
            && maybeToken.get().getUsedAt() == null
            && maybeToken.get().getExpiresAt().isAfter(LocalDateTime.now());

    if (!valid) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token.");
    }

    PasswordResetToken passwordResetToken = maybeToken.get();
    User user = passwordResetToken.getUser();
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    userRepository.save(user);

    passwordResetToken.setUsedAt(LocalDateTime.now());
    passwordResetTokenRepository.save(passwordResetToken);
  }

  private String hashToken(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available on this JVM", e);
    }
  }
}
