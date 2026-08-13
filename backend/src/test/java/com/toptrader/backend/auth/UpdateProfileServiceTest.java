package com.toptrader.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/** Covers ADR 0047's edit-profile flow (PATCH /auth/me). */
@ExtendWith(MockitoExtension.class)
class UpdateProfileServiceTest {

  private static final Long USER_ID = 1L;
  private static final String CURRENT_SESSION_ID = "current-session-id";

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private EmailVerificationService emailVerificationService;
  @Mock private SessionEstablisher sessionEstablisher;
  @Mock private SessionRegistry sessionRegistry;
  @Mock private HttpServletRequest httpRequest;
  @Mock private HttpServletResponse httpResponse;
  @Mock private HttpSession httpSession;

  private UpdateProfileService service;
  private User user;

  @BeforeEach
  void setUp() {
    service =
        new UpdateProfileService(
            userRepository,
            passwordEncoder,
            emailVerificationService,
            sessionEstablisher,
            sessionRegistry);
    user = new User("trader@example.com", "trader", "old-hash", new BigDecimal("500.00"));
    ReflectionTestUtils.setField(user, "id", USER_ID);
  }

  private void stubCurrentSession() {
    when(httpRequest.getSession()).thenReturn(httpSession);
    when(httpSession.getId()).thenReturn(CURRENT_SESSION_ID);
  }

  private UpdateProfileRequest request(
      String email, String username, String password, String avatarKey) {
    return new UpdateProfileRequest(email, username, password, avatarKey);
  }

  @Test
  void updateProfile_updatesAllFieldsAndEstablishesSession_whenRequestValid() {
    stubCurrentSession();
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
    when(userRepository.existsByEmailAndIdNot("new@example.com", USER_ID)).thenReturn(false);
    when(userRepository.existsByUsernameAndIdNot("newname", USER_ID)).thenReturn(false);
    when(passwordEncoder.encode("newPassword123")).thenReturn("new-hash");
    when(userRepository.save(user)).thenReturn(user);
    when(sessionRegistry.getAllPrincipals()).thenReturn(List.of());

    UpdateProfileRequest req =
        request("new@example.com", "newname", "newPassword123", "adventurer-01");

    UserSummary summary = service.updateProfile(USER_ID, req, httpRequest, httpResponse);

    assertThat(user.getEmail()).isEqualTo("new@example.com");
    assertThat(user.getUsername()).isEqualTo("newname");
    assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    assertThat(user.getAvatarKey()).isEqualTo("adventurer-01");
    assertThat(user.getEmailVerifiedAt()).isNull();
    assertThat(summary.avatarKey()).isEqualTo("adventurer-01");
    verify(userRepository).save(user);
    verify(sessionEstablisher).establishSession(user, httpRequest, httpResponse);
  }

  @Test
  void updateProfile_throwsConflict_whenEmailAlreadyInUse() {
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
    when(userRepository.existsByEmailAndIdNot("taken@example.com", USER_ID)).thenReturn(true);

    UpdateProfileRequest req = request("taken@example.com", null, null, null);

    assertThatThrownBy(() -> service.updateProfile(USER_ID, req, httpRequest, httpResponse))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void updateProfile_throwsConflict_whenUsernameAlreadyInUse() {
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
    when(userRepository.existsByUsernameAndIdNot("taken", USER_ID)).thenReturn(true);

    UpdateProfileRequest req = request(null, "taken", null, null);

    assertThatThrownBy(() -> service.updateProfile(USER_ID, req, httpRequest, httpResponse))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void updateProfile_resendsVerificationAndInvalidatesOtherSessions_whenEmailChanges() {
    stubCurrentSession();
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
    when(userRepository.existsByEmailAndIdNot("new@example.com", USER_ID)).thenReturn(false);
    when(userRepository.save(user)).thenReturn(user);

    UserPrincipal principal = new UserPrincipal(user);
    when(sessionRegistry.getAllPrincipals()).thenReturn(List.of(principal));
    SessionInformation currentSession =
        new SessionInformation(principal, CURRENT_SESSION_ID, new Date());
    SessionInformation otherSession =
        new SessionInformation(principal, "other-session-id", new Date());
    when(sessionRegistry.getAllSessions(principal, false))
        .thenReturn(List.of(currentSession, otherSession));

    UpdateProfileRequest req = request("new@example.com", null, null, null);

    service.updateProfile(USER_ID, req, httpRequest, httpResponse);

    assertThat(user.getEmailVerifiedAt()).isNull();
    verify(emailVerificationService).sendVerificationEmail(user);
    assertThat(otherSession.isExpired()).isTrue();
    assertThat(currentSession.isExpired()).isFalse();
  }

  @Test
  void updateProfile_invalidatesOtherSessions_whenPasswordChanges() {
    stubCurrentSession();
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
    when(passwordEncoder.encode("newPassword123")).thenReturn("new-hash");
    when(userRepository.save(user)).thenReturn(user);

    UserPrincipal principal = new UserPrincipal(user);
    when(sessionRegistry.getAllPrincipals()).thenReturn(List.of(principal));
    SessionInformation currentSession =
        new SessionInformation(principal, CURRENT_SESSION_ID, new Date());
    SessionInformation otherSession =
        new SessionInformation(principal, "other-session-id", new Date());
    when(sessionRegistry.getAllSessions(principal, false))
        .thenReturn(List.of(currentSession, otherSession));

    UpdateProfileRequest req = request(null, null, "newPassword123", null);

    service.updateProfile(USER_ID, req, httpRequest, httpResponse);

    assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    verify(emailVerificationService, never()).sendVerificationEmail(any(User.class));
    assertThat(otherSession.isExpired()).isTrue();
    assertThat(currentSession.isExpired()).isFalse();
  }

  @Test
  void updateProfile_throwsForbidden_whenAccountIsDemo() {
    ReflectionTestUtils.setField(user, "isDemo", true);
    when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));

    UpdateProfileRequest req = request("new@example.com", null, null, null);

    assertThatThrownBy(() -> service.updateProfile(USER_ID, req, httpRequest, httpResponse))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);

    verify(userRepository, never()).save(any(User.class));
    verify(sessionEstablisher, never()).establishSession(any(), any(), any());
  }
}
