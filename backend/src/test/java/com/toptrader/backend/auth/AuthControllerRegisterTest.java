package com.toptrader.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserRepository;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
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
class AuthControllerRegisterTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void register_withValidRequest_createsAccountAndAuthenticatesSession() throws Exception {
    String requestBody =
        """
        {"email":"alice@example.com","username":"alice","password":"password123"}
        """;

    MvcResult result =
        mockMvc
            .perform(
                post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("alice@example.com"))
            .andExpect(jsonPath("$.username").value("alice"))
            .andReturn();

    User saved = userRepository.findByEmail("alice@example.com").orElseThrow();
    assertThat(saved.getCashBalance()).isEqualByComparingTo("500.00");
    assertThat(passwordEncoder.matches("password123", saved.getPasswordHash())).isTrue();

    HttpSession session = result.getRequest().getSession(false);
    assertThat(session).isNotNull();
    SecurityContext securityContext =
        (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");
    assertThat(securityContext).isNotNull();
    assertThat(securityContext.getAuthentication().isAuthenticated()).isTrue();
  }

  @Test
  void register_withDuplicateEmail_returns409AndDoesNotCreateSecondAccount() throws Exception {
    userRepository.save(
        new User(
            "bob@example.com",
            "bob",
            passwordEncoder.encode("password123"),
            new BigDecimal("500.00")));

    String requestBody =
        """
        {"email":"bob@example.com","username":"bob2","password":"password123"}
        """;

    mockMvc
        .perform(
            post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("Email already in use"));

    assertThat(userRepository.count()).isEqualTo(1);
  }

  @Test
  void register_withInvalidEmailAndShortPassword_returns400WithFieldErrorsAndNoAccountCreated()
      throws Exception {
    String requestBody =
        """
        {"email":"not-an-email","username":"carol","password":"short"}
        """;

    mockMvc
        .perform(
            post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.email").exists())
        .andExpect(jsonPath("$.errors.password").exists());

    assertThat(userRepository.count()).isEqualTo(0);
  }
}
