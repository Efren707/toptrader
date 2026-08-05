package com.toptrader.backend.auth;

import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmailVerificationService {
  private final EmailVerificationTokenRepository emailVerificationTokenRepository;
  private final UserRepository userRepository;

  public EmailVerificationService(
      EmailVerificationTokenRepository emailVerificationTokenRepository,
      UserRepository userRepository) {
    this.emailVerificationTokenRepository = emailVerificationTokenRepository;
    this.userRepository = userRepository;
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
