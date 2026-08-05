package com.toptrader.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.toptrader.backend.email.EmailSender;
import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserPrincipal;
import com.toptrader.backend.user.UserRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/** Covers ADR 0036's request-reset / reset-password flow. */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

  private static final String EMAIL = "trader@example.com";
  private static final String FRONTEND_ORIGIN = "http://localhost:4200";

  @Mock private UserRepository userRepository;
  @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private EmailSender emailSender;
  @Mock private SessionRegistry sessionRegistry;

  private User user;

  @BeforeEach
  void setUp() {
    user = new User(EMAIL, "trader", "old-hash", BigDecimal.ZERO);
    ReflectionTestUtils.setField(user, "id", 1L);
  }

  private PasswordResetService serviceWithEmailSender() {
    PasswordResetService service =
        new PasswordResetService(
            userRepository,
            passwordResetTokenRepository,
            passwordEncoder,
            Optional.of(emailSender),
            sessionRegistry);
    ReflectionTestUtils.setField(service, "frontendOrigin", FRONTEND_ORIGIN);
    return service;
  }

  private PasswordResetService serviceWithoutEmailSender() {
    return new PasswordResetService(
        userRepository,
        passwordResetTokenRepository,
        passwordEncoder,
        Optional.empty(),
        sessionRegistry);
  }

  private static String sha256Hex(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  void resetRequest_throwsServiceUnavailable_whenNoEmailSenderConfigured() {
    PasswordResetService service = serviceWithoutEmailSender();

    assertThatThrownBy(() -> service.resetRequest(EMAIL))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

    verifyNoInteractions(userRepository, passwordResetTokenRepository);
  }

  @Test
  void resetRequest_savesTokenAndEmailsLink_whenUserExists() {
    PasswordResetService service = serviceWithEmailSender();
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

    service.resetRequest(EMAIL);

    ArgumentCaptor<PasswordResetToken> tokenCaptor =
        ArgumentCaptor.forClass(PasswordResetToken.class);
    verify(passwordResetTokenRepository).save(tokenCaptor.capture());
    PasswordResetToken savedToken = tokenCaptor.getValue();
    assertThat(savedToken.getUser()).isEqualTo(user);
    assertThat(savedToken.getExpiresAt())
        .isAfter(LocalDateTime.now().plusMinutes(29))
        .isBefore(LocalDateTime.now().plusMinutes(31));

    ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(emailSender).send(eq(EMAIL), any(String.class), bodyCaptor.capture());
    String body = bodyCaptor.getValue();
    assertThat(body).contains(FRONTEND_ORIGIN + "/reset-password?token=");
    String rawToken = body.substring(body.indexOf("token=") + "token=".length());
    assertThat(sha256Hex(rawToken)).isEqualTo(savedToken.getTokenHash());
  }

  @Test
  void resetRequest_doesNothing_whenUserDoesNotExist() {
    PasswordResetService service = serviceWithEmailSender();
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

    service.resetRequest(EMAIL);

    verifyNoInteractions(passwordResetTokenRepository, emailSender);
  }

  @Test
  void resetPassword_updatesPasswordAndMarksTokenUsed_whenTokenValid() {
    PasswordResetService service = serviceWithEmailSender();
    String rawToken = "valid-raw-token";
    String tokenHash = sha256Hex(rawToken);
    PasswordResetToken resetToken =
        new PasswordResetToken(user, tokenHash, LocalDateTime.now().plusMinutes(10));
    when(passwordResetTokenRepository.findByTokenHash(tokenHash))
        .thenReturn(Optional.of(resetToken));
    when(passwordEncoder.encode("newPassword123")).thenReturn("new-hash");
    when(sessionRegistry.getAllPrincipals()).thenReturn(List.of());

    service.resetPassword(rawToken, "newPassword123");

    assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    verify(userRepository).save(user);
    assertThat(resetToken.getUsedAt()).isNotNull();
    verify(passwordResetTokenRepository).save(resetToken);
  }

  @Test
  void resetPassword_expiresOnlyTheResetUsersOtherSessions_whenTokenValid() {
    PasswordResetService service = serviceWithEmailSender();
    String rawToken = "valid-raw-token";
    String tokenHash = sha256Hex(rawToken);
    PasswordResetToken resetToken =
        new PasswordResetToken(user, tokenHash, LocalDateTime.now().plusMinutes(10));
    when(passwordResetTokenRepository.findByTokenHash(tokenHash))
        .thenReturn(Optional.of(resetToken));
    when(passwordEncoder.encode("newPassword123")).thenReturn("new-hash");

    User otherUser = new User("other@example.com", "other", "other-hash", BigDecimal.ZERO);
    ReflectionTestUtils.setField(otherUser, "id", 2L);
    UserPrincipal thisUserPrincipal = new UserPrincipal(user);
    UserPrincipal otherUserPrincipal = new UserPrincipal(otherUser);
    when(sessionRegistry.getAllPrincipals())
        .thenReturn(List.of(thisUserPrincipal, otherUserPrincipal));
    SessionInformation thisUserSession =
        new SessionInformation(thisUserPrincipal, "this-user-session-id", new Date());
    when(sessionRegistry.getAllSessions(thisUserPrincipal, false))
        .thenReturn(List.of(thisUserSession));

    service.resetPassword(rawToken, "newPassword123");

    assertThat(thisUserSession.isExpired()).isTrue();
    verify(sessionRegistry, never()).getAllSessions(eq(otherUserPrincipal), any(Boolean.class));
  }

  @Test
  void resetPassword_throwsBadRequest_whenTokenNotFound() {
    PasswordResetService service = serviceWithEmailSender();
    when(passwordResetTokenRepository.findByTokenHash(any(String.class)))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.resetPassword("unknown-token", "newPassword123"))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);

    verifyNoInteractions(userRepository);
    verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
  }

  @Test
  void resetPassword_throwsBadRequest_whenTokenAlreadyUsed() {
    PasswordResetService service = serviceWithEmailSender();
    String rawToken = "used-token";
    String tokenHash = sha256Hex(rawToken);
    PasswordResetToken resetToken =
        new PasswordResetToken(user, tokenHash, LocalDateTime.now().plusMinutes(10));
    resetToken.setUsedAt(LocalDateTime.now().minusMinutes(5));
    when(passwordResetTokenRepository.findByTokenHash(tokenHash))
        .thenReturn(Optional.of(resetToken));

    assertThatThrownBy(() -> service.resetPassword(rawToken, "newPassword123"))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);

    verifyNoInteractions(userRepository);
    verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
  }

  @Test
  void resetPassword_throwsBadRequest_whenTokenExpired() {
    PasswordResetService service = serviceWithEmailSender();
    String rawToken = "expired-token";
    String tokenHash = sha256Hex(rawToken);
    PasswordResetToken resetToken =
        new PasswordResetToken(user, tokenHash, LocalDateTime.now().minusMinutes(1));
    when(passwordResetTokenRepository.findByTokenHash(tokenHash))
        .thenReturn(Optional.of(resetToken));

    assertThatThrownBy(() -> service.resetPassword(rawToken, "newPassword123"))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);

    verifyNoInteractions(userRepository);
    verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
  }
}
