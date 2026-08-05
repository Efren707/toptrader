package com.toptrader.backend.auth;

import com.toptrader.backend.email.EmailSender;
import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserPrincipal;
import com.toptrader.backend.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Registration flow per ADR 0004; starting cash constant per US-3 (data-model.md). */
@Service
public class RegistrationService {
  private static final int VERIFICATION_TOKEN_TTL_MINUTES = 30;
  private static final BigDecimal STARTING_CASH_BALANCE = new BigDecimal("500.00");

  @Value("${toptrader.frontend-origin:http://localhost:4200}")
  private String frontendOrigin;

  private final UserRepository userRepository;
  private final EmailVerificationTokenRepository emailVerificationTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final Optional<EmailSender> emailSender;
  private final SecurityContextRepository securityContextRepository =
      new HttpSessionSecurityContextRepository();

  public RegistrationService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      EmailVerificationTokenRepository emailVerificationTokenRepository,
      Optional<EmailSender> emailSender) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.emailVerificationTokenRepository = emailVerificationTokenRepository;
    this.emailSender = emailSender;
  }

  @Transactional
  public UserSummary register(
      RegisterRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    if (userRepository.existsByEmail(request.email())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
    }
    if (userRepository.existsByUsername(request.username())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already in use");
    }

    User user =
        new User(
            request.email(),
            request.username(),
            passwordEncoder.encode(request.password()),
            STARTING_CASH_BALANCE);
    user = userRepository.save(user);
    establishSession(user, httpRequest, httpResponse);

    if (emailSender.isPresent()) {
      sendVerificationEmail(user);
    }

    return UserSummary.from(user);
  }

  private void sendVerificationEmail(User user) {
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

  private String hashToken(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available on this JVM", e);
    }
  }

  private void establishSession(
      User user, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    UserPrincipal principal = new UserPrincipal(user);
    Authentication authentication =
        UsernamePasswordAuthenticationToken.authenticated(
            principal, principal.getPassword(), principal.getAuthorities());

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    securityContextRepository.saveContext(context, httpRequest, httpResponse);
  }
}
