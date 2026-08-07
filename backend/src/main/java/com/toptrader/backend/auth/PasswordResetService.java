package com.toptrader.backend.auth;

import com.toptrader.backend.email.EmailSender;
import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserPrincipal;
import com.toptrader.backend.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PasswordResetService {

  private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

  private static final int RESET_TOKEN_TTL_MINUTES = 30;

  private final UserRepository userRepository;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final Optional<EmailSender> emailSender;
  private final SessionRegistry sessionRegistry;

  @Value("${toptrader.frontend-origin:http://localhost:4200}")
  private String frontendOrigin;

  public PasswordResetService(
      UserRepository userRepository,
      PasswordResetTokenRepository passwordResetTokenRepository,
      PasswordEncoder passwordEncoder,
      Optional<EmailSender> emailSender,
      SessionRegistry sessionRegistry) {
    this.userRepository = userRepository;
    this.passwordResetTokenRepository = passwordResetTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.emailSender = emailSender;
    this.sessionRegistry = sessionRegistry;
  }

  public void resetRequest(String email) {
    if (emailSender.isEmpty()) {
      log.atWarn().addKeyValue("reason", "Email sender unavailable").log("Password reset failed");
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

      log.atInfo()
          .addKeyValue("userId", user.get().getId())
          .log("Password reset request succeeded");
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
      log.atWarn()
          .addKeyValue("reason", "Invalid or expired reset token.")
          .log("Password reset failed");
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token.");
    }

    PasswordResetToken passwordResetToken = maybeToken.get();
    User user = passwordResetToken.getUser();
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    userRepository.save(user);

    List<Object> principals = sessionRegistry.getAllPrincipals();
    for (Object principal : principals) {
      if (principal instanceof UserPrincipal userPrincipal
          && userPrincipal.getUser().getId().equals(user.getId())) {
        for (SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
          session.expireNow();
        }
      }
    }

    passwordResetToken.setUsedAt(LocalDateTime.now());
    log.atInfo().addKeyValue("userId", user.getId()).log("Password reset succeeded");
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
