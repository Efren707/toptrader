package com.toptrader.backend.auth;

import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserPrincipal;
import com.toptrader.backend.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
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

  private static final BigDecimal STARTING_CASH_BALANCE = new BigDecimal("500.00");

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final SecurityContextRepository securityContextRepository =
      new HttpSessionSecurityContextRepository();

  public RegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

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

    return UserSummary.from(user);
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
