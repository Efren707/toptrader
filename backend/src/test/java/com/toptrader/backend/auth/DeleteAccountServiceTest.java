package com.toptrader.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.toptrader.backend.trading.HoldingRepository;
import com.toptrader.backend.trading.TransactionRepository;
import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserPrincipal;
import com.toptrader.backend.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/** Covers ADR 0048's cascade account-deletion flow (DELETE /auth/me). */
@ExtendWith(MockitoExtension.class)
class DeleteAccountServiceTest {

  private static final Long USER_ID = 1L;

  @Mock private UserRepository userRepository;
  @Mock private TransactionRepository transactionRepository;
  @Mock private HoldingRepository holdingRepository;
  @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
  @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
  @Mock private SessionRegistry sessionRegistry;
  @Mock private HttpServletRequest httpRequest;
  @Mock private HttpServletResponse httpResponse;
  @Mock private HttpSession httpSession;

  private DeleteAccountService service;
  private User user;

  @BeforeEach
  void setUp() {
    service =
        new DeleteAccountService(
            userRepository,
            transactionRepository,
            holdingRepository,
            emailVerificationTokenRepository,
            passwordResetTokenRepository,
            sessionRegistry);
    user = new User("trader@example.com", "trader", "hash", new BigDecimal("500.00"));
    ReflectionTestUtils.setField(user, "id", USER_ID);
    UserPrincipal principal = new UserPrincipal(user);
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void deleteAccount_cascadesAllUserDataAndEndsSessions_whenAccountValid() {
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
    when(sessionRegistry.getAllPrincipals()).thenReturn(List.of());
    when(httpRequest.getSession(false)).thenReturn(httpSession);

    service.deleteAccount(USER_ID, httpRequest, httpResponse);

    InOrder order = inOrder(transactionRepository, holdingRepository, userRepository);
    order.verify(transactionRepository).deleteByUser(user);
    order.verify(holdingRepository).deleteByUser(user);
    order.verify(userRepository).delete(user);
    verify(emailVerificationTokenRepository).deleteByUser(user);
    verify(passwordResetTokenRepository).deleteByUser(user);
    verify(httpSession).invalidate();
    verify(httpResponse).addCookie(any());
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void deleteAccount_expiresEveryOneOfTheUsersSessions_includingTheCurrentOne() {
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
    when(httpRequest.getSession(false)).thenReturn(httpSession);

    UserPrincipal principal = new UserPrincipal(user);
    when(sessionRegistry.getAllPrincipals()).thenReturn(List.of(principal));
    SessionInformation currentSession =
        new SessionInformation(principal, "current-session-id", new Date());
    SessionInformation otherSession =
        new SessionInformation(principal, "other-session-id", new Date());
    when(sessionRegistry.getAllSessions(principal, false))
        .thenReturn(List.of(currentSession, otherSession));

    service.deleteAccount(USER_ID, httpRequest, httpResponse);

    assertThat(currentSession.isExpired()).isTrue();
    assertThat(otherSession.isExpired()).isTrue();
  }

  @Test
  void deleteAccount_throwsForbidden_whenAccountIsDemo() {
    ReflectionTestUtils.setField(user, "isDemo", true);
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> service.deleteAccount(USER_ID, httpRequest, httpResponse))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);

    verify(userRepository, never()).delete(any(User.class));
    verify(transactionRepository, never()).deleteByUser(any(User.class));
    verify(holdingRepository, never()).deleteByUser(any(User.class));
  }
}
