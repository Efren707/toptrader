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

  public AuthController(RegistrationService registrationService, LoginService loginService) {
    this.registrationService = registrationService;
    this.loginService = loginService;
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

  @GetMapping("/session")
  public ResponseEntity<UserSummary> getSession(@AuthenticationPrincipal UserPrincipal principal) {
    return ResponseEntity.ok(UserSummary.from(principal.getUser()));
  }
}
