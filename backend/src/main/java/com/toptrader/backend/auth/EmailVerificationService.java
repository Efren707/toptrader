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
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmailVerificationService {
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
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Invalid or expired email verification token.");
    }

    EmailVerificationToken emailVerificationToken = maybeToken.get();
    User user = emailVerificationToken.getUser();
    user.setEmailVerifiedAt(LocalDateTime.now());
    userRepository.save(user);

    emailVerificationToken.setUsedAt(LocalDateTime.now());
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
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Email Sender is currently unavailable.");
    }

    Optional<User> user = this.userRepository.findByEmail(email);

    if (user.isPresent() && user.get().getEmailVerifiedAt() == null) {
      sendVerificationEmail(user.get());
    }
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
