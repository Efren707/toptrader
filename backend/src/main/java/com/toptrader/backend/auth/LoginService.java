package com.toptrader.backend.auth;

import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserPrincipal;
import com.toptrader.backend.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Login flow per ADR 0004; brute-force lockout tracked inline here (see ADR 0004 amendment). */
@Service
public class LoginService {

  private static final Logger log = LoggerFactory.getLogger(LoginService.class);

  private static final int MAX_FAILED_ATTEMPTS = 5;
  private static final long LOCKOUT_MINUTES = 15;

  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  private final SessionEstablisher sessionEstablisher;

  public LoginService(
      AuthenticationManager authenticationManager,
      UserRepository userRepository,
      SessionEstablisher sessionEstablisher) {
    this.authenticationManager = authenticationManager;
    this.userRepository = userRepository;
    this.sessionEstablisher = sessionEstablisher;
  }

  public UserSummary login(
      LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    Authentication authentication;
    try {
      authentication =
          authenticationManager.authenticate(
              UsernamePasswordAuthenticationToken.unauthenticated(
                  request.email(), request.password()));
    } catch (AuthenticationException e) {
      recordFailedAttempt(request.email());
      log.atWarn()
          .addKeyValue("email", request.email())
          .addKeyValue("reason", e.getClass().getSimpleName())
          .log("Login failed");
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
    User user = principal.getUser();
    resetFailedAttempts(user);

    this.sessionEstablisher.establishSession(user, httpRequest, httpResponse);
    log.atInfo().addKeyValue("userId", user.getId()).log("Login succeeded");
    return UserSummary.from(user);
  }

  private void recordFailedAttempt(String email) {
    Optional<User> maybeUser = userRepository.findByEmail(email);
    if (maybeUser.isEmpty()) {
      return;
    }

    User user = maybeUser.get();
    boolean currentlyLocked =
        user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now());
    if (currentlyLocked) {
      return;
    }

    user.setFailedAttempts(user.getFailedAttempts() + 1);
    if (user.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
      user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
      log.atWarn()
          .addKeyValue("userId", user.getId())
          .addKeyValue("email", email)
          .log("Account locked after repeated failed login attempts");
    }
    userRepository.save(user);
  }

  private void resetFailedAttempts(User user) {
    user.setFailedAttempts(0);
    user.setLockedUntil(null);
    userRepository.save(user);
  }
}
