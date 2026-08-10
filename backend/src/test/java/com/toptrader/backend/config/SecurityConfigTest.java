package com.toptrader.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserRepository;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies the real XSRF-TOKEN cookie/header round-trip — deliberately not using
 * SecurityMockMvcRequestPostProcessors.csrf(), which fakes a valid token and bypasses the actual
 * flow entirely, the reason the cross-subdomain cookie-domain bug this covers went uncaught.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityConfigTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void csrfCookie_hasNoDomainAttribute_byDefault() throws Exception {
    MvcResult result =
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk()).andReturn();

    Cookie csrfCookie = result.getResponse().getCookie("XSRF-TOKEN");

    assertThat(csrfCookie).isNotNull();
    assertThat(csrfCookie.getDomain()).isNullOrEmpty();
  }

  @Test
  void logout_withRealCsrfCookieAndHeader_succeeds() throws Exception {
    seedUser("csrf-logout@example.com", "csrflogout", "password123");
    MockHttpSession session = loginAndGetSession("csrf-logout@example.com", "password123");

    Cookie csrfCookie = fetchCsrfCookie(session);

    mockMvc
        .perform(
            post("/auth/logout")
                .session(session)
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue()))
        .andExpect(status().isNoContent());
  }

  @Test
  void logout_withCsrfCookieButNoHeader_isForbidden() throws Exception {
    seedUser("csrf-noheader@example.com", "csrfnoheader", "password123");
    MockHttpSession session = loginAndGetSession("csrf-noheader@example.com", "password123");

    Cookie csrfCookie = fetchCsrfCookie(session);

    mockMvc
        .perform(post("/auth/logout").session(session).cookie(csrfCookie))
        .andExpect(status().isForbidden());
  }

  private Cookie fetchCsrfCookie(MockHttpSession session) throws Exception {
    MvcResult result =
        mockMvc
            .perform(get("/actuator/health").session(session))
            .andExpect(status().isOk())
            .andReturn();
    Cookie csrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
    assertThat(csrfCookie).isNotNull();
    return csrfCookie;
  }

  private User seedUser(String email, String username, String rawPassword) {
    return userRepository.save(
        new User(email, username, passwordEncoder.encode(rawPassword), new BigDecimal("500.00")));
  }

  private MockHttpSession loginAndGetSession(String email, String password) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequestBody(email, password)))
            .andExpect(status().isOk())
            .andReturn();
    return (MockHttpSession) result.getRequest().getSession(false);
  }

  private String loginRequestBody(String email, String password) {
    return """
        {"email":"%s","password":"%s"}
        """
        .formatted(email, password);
  }
}
