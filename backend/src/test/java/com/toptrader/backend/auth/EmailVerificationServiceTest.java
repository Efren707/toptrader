package com.toptrader.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.toptrader.backend.email.EmailSender;
import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/** Covers ADR 0037's send/resend verification-email flow (verifyEmail is covered separately). */
@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

  private static final String EMAIL = "trader@example.com";
  private static final String FRONTEND_ORIGIN = "http://localhost:4200";

  @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
  @Mock private UserRepository userRepository;
  @Mock private EmailSender emailSender;

  private User user;

  @BeforeEach
  void setUp() {
    user = new User(EMAIL, "trader", "hash", BigDecimal.ZERO);
    ReflectionTestUtils.setField(user, "id", 1L);
  }

  private EmailVerificationService serviceWithEmailSender() {
    EmailVerificationService service =
        new EmailVerificationService(
            emailVerificationTokenRepository, userRepository, Optional.of(emailSender));
    ReflectionTestUtils.setField(service, "frontendOrigin", FRONTEND_ORIGIN);
    return service;
  }

  private EmailVerificationService serviceWithoutEmailSender() {
    return new EmailVerificationService(
        emailVerificationTokenRepository, userRepository, Optional.empty());
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
  void sendVerificationEmail_savesTokenAndEmailsLink_whenEmailSenderConfigured() {
    EmailVerificationService service = serviceWithEmailSender();

    service.sendVerificationEmail(user);

    ArgumentCaptor<EmailVerificationToken> tokenCaptor =
        ArgumentCaptor.forClass(EmailVerificationToken.class);
    verify(emailVerificationTokenRepository).save(tokenCaptor.capture());
    EmailVerificationToken savedToken = tokenCaptor.getValue();
    assertThat(savedToken.getUser()).isEqualTo(user);
    assertThat(savedToken.getExpiresAt())
        .isAfter(LocalDateTime.now().plusMinutes(29))
        .isBefore(LocalDateTime.now().plusMinutes(31));

    ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(emailSender).send(eq(EMAIL), any(String.class), bodyCaptor.capture());
    String body = bodyCaptor.getValue();
    assertThat(body).contains(FRONTEND_ORIGIN + "/verify-email?token=");
    String rawToken = body.substring(body.indexOf("token=") + "token=".length());
    assertThat(sha256Hex(rawToken)).isEqualTo(savedToken.getTokenHash());
  }

  @Test
  void sendVerificationEmail_doesNothing_whenNoEmailSenderConfigured() {
    EmailVerificationService service = serviceWithoutEmailSender();

    service.sendVerificationEmail(user);

    verifyNoInteractions(emailVerificationTokenRepository);
  }

  @Test
  void resendVerification_throwsServiceUnavailable_whenNoEmailSenderConfigured() {
    EmailVerificationService service = serviceWithoutEmailSender();

    assertThatThrownBy(() -> service.resendVerification(EMAIL))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

    verifyNoInteractions(userRepository, emailVerificationTokenRepository);
  }

  @Test
  void resendVerification_savesTokenAndEmailsLink_whenUserExistsAndUnverified() {
    EmailVerificationService service = serviceWithEmailSender();
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

    service.resendVerification(EMAIL);

    verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
    verify(emailSender).send(eq(EMAIL), any(String.class), any(String.class));
  }

  @Test
  void resendVerification_doesNothing_whenUserDoesNotExist() {
    EmailVerificationService service = serviceWithEmailSender();
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

    service.resendVerification(EMAIL);

    verifyNoInteractions(emailVerificationTokenRepository, emailSender);
  }

  @Test
  void resendVerification_doesNothing_whenUserAlreadyVerified() {
    EmailVerificationService service = serviceWithEmailSender();
    user.setEmailVerifiedAt(LocalDateTime.now().minusDays(1));
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

    service.resendVerification(EMAIL);

    verifyNoInteractions(emailVerificationTokenRepository, emailSender);
  }
}
