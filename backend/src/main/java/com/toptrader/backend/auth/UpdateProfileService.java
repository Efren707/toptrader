package com.toptrader.backend.auth;

import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserPrincipal;
import com.toptrader.backend.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UpdateProfileService {
  private static final Logger log = LoggerFactory.getLogger(UpdateProfileService.class);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailVerificationService emailVerificationService;
  private final SessionEstablisher sessionEstablisher;
  private final SessionRegistry sessionRegistry;

  public UpdateProfileService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      EmailVerificationService emailVerificationService,
      SessionEstablisher sessionEstablisher,
      SessionRegistry sessionRegistry) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.emailVerificationService = emailVerificationService;
    this.sessionEstablisher = sessionEstablisher;
    this.sessionRegistry = sessionRegistry;
  }

  @Transactional
  public UserSummary updateProfile(
      Long userId,
      UpdateProfileRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {

    User user =
        this.userRepository
            .findByIdForUpdate(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    if (user.isDemo()) {
      log.atWarn()
          .addKeyValue("userId", userId)
          .addKeyValue("reason", "Demo account is read-only")
          .log("Update profile failed");
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Demo accounts are read-only and cannot change profile details");
    }

    if (userRepository.existsByEmailAndIdNot(request.email(), userId)) {
      log.atWarn()
          .addKeyValue("email", request.email())
          .addKeyValue("reason", "Email already in use")
          .log("Update profile failed");
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
    }

    if (userRepository.existsByUsernameAndIdNot(request.username(), userId)) {
      log.atWarn()
          .addKeyValue("username", request.username())
          .addKeyValue("reason", "Username already in use")
          .log("Update profile failed");
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already in use");
    }

    if (request.email() != null && !request.email().isBlank()) {
      user.setEmail(request.email());
      user.setEmailVerifiedAt(null);
      this.emailVerificationService.sendVerificationEmail(user);
      resetSession(userId, httpRequest.getSession().getId());
    }

    if (request.username() != null && !request.username().isBlank()) {
      user.setUsername(request.username());
    }

    if (request.avatarKey() != null && !request.avatarKey().isBlank()) {
      user.setAvatarKey(request.avatarKey());
    }

    if (request.password() != null && !request.password().isBlank()) {
      user.setPasswordHash(passwordEncoder.encode(request.password()));
      resetSession(userId, httpRequest.getSession().getId());
    }

    user = userRepository.save(user);
    this.sessionEstablisher.establishSession(user, httpRequest, httpResponse);

    log.atInfo().addKeyValue("userId", user.getId()).log("Update profile succeeded");
    return UserSummary.from(user);
  }

  public void resetSession(Long userId, String sessionId) {
    List<Object> principals = sessionRegistry.getAllPrincipals();

    for (Object principal : principals) {
      if (principal instanceof UserPrincipal userPrincipal
          && userPrincipal.getUser().getId().equals(userId)) {
        for (SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
          if (!session.getSessionId().equals(sessionId)) {
            session.expireNow();
          }
        }
      }
    }
  }
}
