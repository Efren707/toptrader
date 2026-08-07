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
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmailVerificationService {

  private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

  private final EmailVerificationTokenRepository emailVerificationTokenRepository;
  private final UserRepository userRepository;
  private final Optional<EmailSender> emailSender;

  private static final int VERIFICATION_TOKEN_TTL_MINUTES = 30;

  @Value("${toptrader.frontend-origin:http://localhost:4200}")
  private String frontendOrigin;

  public EmailVerificationService(
      EmailVerificationTokenRepository emailVerificationTokenRepository,
      UserRepository userRepository,
      Optional<EmailSender> emailSender) {
    this.emailVerificationTokenRepository = emailVerificationTokenRepository;
    this.userRepository = userRepository;
    this.emailSender = emailSender;
  }

  public void verifyEmail(String rawToken) {
    String tokenHash = hashToken(rawToken);
    Optional<EmailVerificationToken> maybeToken =
        emailVerificationTokenRepository.findByTokenHash(tokenHash);

    boolean valid =
        maybeToken.isPresent()
            && maybeToken.get().getUsedAt() == null
            && maybeToken.get().getExpiresAt().isAfter(LocalDateTime.now());

    if (!valid) {
      log.atWarn()
          .addKeyValue("reason", "Invalid or expired email verification token.")
          .log("Email verification failed");
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Invalid or expired email verification token.");
    }

    EmailVerificationToken emailVerificationToken = maybeToken.get();
    User user = emailVerificationToken.getUser();
    user.setEmailVerifiedAt(LocalDateTime.now());
    userRepository.save(user);

    emailVerificationToken.setUsedAt(LocalDateTime.now());
    log.atInfo().addKeyValue("userId", user.getId()).log("Email verification succeeded");
    emailVerificationTokenRepository.save(emailVerificationToken);
  }

  public void sendVerificationEmail(User user) {
    if (emailSender.isEmpty()) {
      return;
    }

    SecureRandom secureRandom = new SecureRandom();
    byte[] tokenBytes = new byte[32];
    secureRandom.nextBytes(tokenBytes);
    String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    String tokenHash = hashToken(rawToken);

    LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(VERIFICATION_TOKEN_TTL_MINUTES);
    EmailVerificationToken emailVerificationToken =
        new EmailVerificationToken(user, tokenHash, expiresAt);
    emailVerificationTokenRepository.save(emailVerificationToken);

    String verifyLink = frontendOrigin + "/verify-email?token=" + rawToken;
    emailSender
        .get()
        .send(
            user.getEmail(),
            "Verify your TopTrader email",
            "Use this link to verify your email: " + verifyLink);
  }

  public void resendVerification(String email) {
    if (emailSender.isEmpty()) {
      log.atWarn()
          .addKeyValue("reason", "Email sender unavailable")
          .log("Resend email verification failed");
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Email Sender is currently unavailable.");
    }

    Optional<User> user = this.userRepository.findByEmail(email);

    if (user.isPresent() && user.get().getEmailVerifiedAt() == null) {
      invalidateOutstandingTokens(user.get());
      sendVerificationEmail(user.get());
      log.atInfo()
          .addKeyValue("userId", user.get().getId())
          .log("Email verification resend succeeded");
    }
  }

  private void invalidateOutstandingTokens(User user) {
    List<EmailVerificationToken> outstanding =
        emailVerificationTokenRepository.findByUserAndUsedAtIsNull(user);
    LocalDateTime now = LocalDateTime.now();
    outstanding.forEach(token -> token.setUsedAt(now));
    emailVerificationTokenRepository.saveAll(outstanding);
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
