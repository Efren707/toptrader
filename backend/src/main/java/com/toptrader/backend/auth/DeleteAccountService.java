package com.toptrader.backend.auth;

import com.toptrader.backend.trading.HoldingRepository;
import com.toptrader.backend.trading.TransactionRepository;
import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserPrincipal;
import com.toptrader.backend.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DeleteAccountService {
  private static final Logger log = LoggerFactory.getLogger(DeleteAccountService.class);

  private final UserRepository userRepository;
  private final TransactionRepository transactionRepository;
  private final HoldingRepository holdingRepository;
  private final EmailVerificationTokenRepository emailVerificationTokenRepository;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final SessionRegistry sessionRegistry;

  public DeleteAccountService(
      UserRepository userRepository,
      TransactionRepository transactionRepository,
      HoldingRepository holdingRepository,
      EmailVerificationTokenRepository emailVerificationTokenRepository,
      PasswordResetTokenRepository passwordResetTokenRepository,
      SessionRegistry sessionRegistry) {
    this.userRepository = userRepository;
    this.transactionRepository = transactionRepository;
    this.holdingRepository = holdingRepository;
    this.emailVerificationTokenRepository = emailVerificationTokenRepository;
    this.passwordResetTokenRepository = passwordResetTokenRepository;
    this.sessionRegistry = sessionRegistry;
  }

  @Transactional
  public void deleteAccount(
      Long userId, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    User user =
        this.userRepository
            .findByIdForUpdate(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    if (user.isDemo()) {
      log.atWarn()
          .addKeyValue("userId", userId)
          .addKeyValue("reason", "Demo account is read-only")
          .log("Delete account failed");
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Demo accounts are read-only and cannot delete account");
    }

    transactionRepository.deleteByUser(user);
    holdingRepository.deleteByUser(user);
    emailVerificationTokenRepository.deleteByUser(user);
    passwordResetTokenRepository.deleteByUser(user);
    userRepository.delete(user);

    endAllSessions(userId, httpRequest, httpResponse);

    log.atInfo().addKeyValue("userId", userId).log("Delete account succeeded");
  }

  private void endAllSessions(
      Long userId, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    for (Object principal : sessionRegistry.getAllPrincipals()) {
      if (principal instanceof UserPrincipal userPrincipal
          && userPrincipal.getUser().getId().equals(userId)) {
        for (SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
          session.expireNow();
        }
      }
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    new SecurityContextLogoutHandler().logout(httpRequest, httpResponse, authentication);
    new CookieClearingLogoutHandler("SESSION").logout(httpRequest, httpResponse, authentication);
  }
}
