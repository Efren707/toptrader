package com.toptrader.backend.auth;

import com.toptrader.backend.user.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final RegistrationService registrationService;
  private final LoginService loginService;
  private final DemoLoginService demoLoginService;
  private final PasswordResetService passwordResetService;
  private final EmailVerificationService emailVerificationService;

  public AuthController(
      RegistrationService registrationService,
      LoginService loginService,
      DemoLoginService demoLoginService,
      PasswordResetService passwordResetService,
      EmailVerificationService emailVerificationService) {
    this.registrationService = registrationService;
    this.loginService = loginService;
    this.demoLoginService = demoLoginService;
    this.passwordResetService = passwordResetService;
    this.emailVerificationService = emailVerificationService;
  }

  @PostMapping("/register")
  public ResponseEntity<UserSummary> register(
      @Valid @RequestBody RegisterRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    UserSummary summary = registrationService.register(request, httpRequest, httpResponse);
    return ResponseEntity.status(HttpStatus.CREATED).body(summary);
  }

  @PostMapping("/login")
  public ResponseEntity<UserSummary> login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    UserSummary summary = loginService.login(request, httpRequest, httpResponse);
    return ResponseEntity.status(HttpStatus.OK).body(summary);
  }

  @PostMapping("/demo-login")
  public ResponseEntity<UserSummary> demoLogin(
      HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    UserSummary summary = demoLoginService.demoLogin(httpRequest, httpResponse);
    return ResponseEntity.status(HttpStatus.OK).body(summary);
  }

  @GetMapping("/session")
  public ResponseEntity<UserSummary> getSession(@AuthenticationPrincipal UserPrincipal principal) {
    return ResponseEntity.ok(UserSummary.from(principal.getUser()));
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    passwordResetService.resetRequest(request.email());
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @PostMapping("/reset-password")
  public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    passwordResetService.resetPassword(request.rawToken(), request.password());
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @PostMapping("/verify-email")
  public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
    emailVerificationService.verifyEmail(request.rawToken());
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @PostMapping("/resend-verification")
  public ResponseEntity<Void> resendVerification(
      @Valid @RequestBody ResendVerificationRequest request) {
    emailVerificationService.resendVerification(request.email());
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
