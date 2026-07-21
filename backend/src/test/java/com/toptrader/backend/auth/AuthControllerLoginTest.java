package com.toptrader.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserRepository;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerLoginTest {

  private static final String GENERIC_ERROR = "Invalid email or password";
  private static final int MAX_FAILED_ATTEMPTS = 5;

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void login_withValidCredentials_returns200AndEstablishesSession() throws Exception {
    seedUser("alice@example.com", "alice", "password123");

    MvcResult result =
        mockMvc
            .perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequestBody("alice@example.com", "password123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("alice@example.com"))
            .andExpect(jsonPath("$.username").value("alice"))
            .andExpect(jsonPath("$.cashBalance").value(500.00))
            .andReturn();

    HttpSession session = result.getRequest().getSession(false);
    assertThat(session).isNotNull();
    SecurityContext securityContext =
        (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");
    assertThat(securityContext).isNotNull();
    assertThat(securityContext.getAuthentication().isAuthenticated()).isTrue();
  }

  @Test
  void login_withWrongPassword_returns401WithGenericMessage() throws Exception {
    seedUser("bob@example.com", "bob", "password123");

    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody("bob@example.com", "wrong-password")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail").value(GENERIC_ERROR));
  }

  @Test
  void login_withUnknownEmail_returns401WithSameGenericMessage() throws Exception {
    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody("nobody@example.com", "whatever123")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail").value(GENERIC_ERROR));
  }

  @Test
  void login_after5FailedAttempts_locksAccountAndRejectsCorrectPassword() throws Exception {
    seedUser("carol@example.com", "carol", "password123");

    for (int i = 0; i < MAX_FAILED_ATTEMPTS; i++) {
      mockMvc
          .perform(
              post("/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(loginRequestBody("carol@example.com", "wrong-password")))
          .andExpect(status().isUnauthorized());
    }

    User locked = userRepository.findByEmail("carol@example.com").orElseThrow();
    assertThat(locked.getFailedAttempts()).isEqualTo(MAX_FAILED_ATTEMPTS);
    assertThat(locked.getLockedUntil()).isNotNull();
    assertThat(locked.getLockedUntil()).isAfter(LocalDateTime.now());

    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody("carol@example.com", "password123")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail").value(GENERIC_ERROR));
  }

  @Test
  void login_withValidCredentialsAfterFailedAttempts_resetsFailedAttempts() throws Exception {
    seedUser("dave@example.com", "dave", "password123");

    for (int i = 0; i < MAX_FAILED_ATTEMPTS - 2; i++) {
      mockMvc
          .perform(
              post("/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(loginRequestBody("dave@example.com", "wrong-password")))
          .andExpect(status().isUnauthorized());
    }

    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody("dave@example.com", "password123")))
        .andExpect(status().isOk());

    User user = userRepository.findByEmail("dave@example.com").orElseThrow();
    assertThat(user.getFailedAttempts()).isEqualTo(0);
    assertThat(user.getLockedUntil()).isNull();
  }

  private User seedUser(String email, String username, String rawPassword) {
    return userRepository.save(
        new User(email, username, passwordEncoder.encode(rawPassword), new BigDecimal("500.00")));
  }

  private String loginRequestBody(String email, String password) {
    return """
        {"email":"%s","password":"%s"}
        """
        .formatted(email, password);
  }
}
