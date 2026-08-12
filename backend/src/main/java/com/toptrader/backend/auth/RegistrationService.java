package com.toptrader.backend.auth;

import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Registration flow per ADR 0004; starting cash constant per US-3 (data-model.md). */
@Service
public class RegistrationService {

  private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

  private static final BigDecimal STARTING_CASH_BALANCE = new BigDecimal("500.00");

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailVerificationService emailVerificationService;
  private final SessionEstablisher sessionEstablisher;

  public RegistrationService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      EmailVerificationService emailVerificationService,
      SessionEstablisher sessionEstablisher) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.emailVerificationService = emailVerificationService;
    this.sessionEstablisher = sessionEstablisher;
  }

  @Transactional
  public UserSummary register(
      RegisterRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    if (userRepository.existsByEmail(request.email())) {
      log.atWarn()
          .addKeyValue("email", request.email())
          .addKeyValue("reason", "Email already in use")
          .log("Register failed");
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
    }
    if (userRepository.existsByUsername(request.username())) {
      log.atWarn()
          .addKeyValue("username", request.username())
          .addKeyValue("reason", "Username already in use")
          .log("Register failed");
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already in use");
    }

    User user =
        new User(
            request.email(),
            request.username(),
            passwordEncoder.encode(request.password()),
            STARTING_CASH_BALANCE);
    user = userRepository.save(user);
    this.sessionEstablisher.establishSession(user, httpRequest, httpResponse);

    this.emailVerificationService.sendVerificationEmail(user);
    log.atInfo().addKeyValue("userId", user.getId()).log("Register succeeded");
    return UserSummary.from(user);
  }
}
