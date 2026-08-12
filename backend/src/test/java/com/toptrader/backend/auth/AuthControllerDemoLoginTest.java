package com.toptrader.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerDemoLoginTest {

  // Must match DemoLoginService.DEMO_EMAIL exactly - can't share a constant across test/main,
  // verified by hand (same constraint noted for the V6 seed migration in
  // docs/tasks/in-progress/demo-account.md). V6 seeds this row permanently, so these tests rely
  // on it existing rather than seeding their own copy.
  private static final String DEMO_EMAIL = "demo@toptrader.dev";

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void demoLogin_withSeededDemoUser_returns200AndEstablishesSession() throws Exception {
    MvcResult result =
        mockMvc
            .perform(post("/auth/demo-login").header("X-Forwarded-For", "203.0.113.30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(DEMO_EMAIL))
            .andExpect(jsonPath("$.isDemo").value(true))
            .andReturn();

    HttpSession session = result.getRequest().getSession(false);
    assertThat(session).isNotNull();
    SecurityContext securityContext =
        (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");
    assertThat(securityContext).isNotNull();
    assertThat(securityContext.getAuthentication().isAuthenticated()).isTrue();
  }

  @Test
  void demoLogin_withoutSeededDemoUser_returns500() throws Exception {
    // V6 seeds holdings/transactions for the demo user too, so those must go first to satisfy
    // their foreign keys before the user row itself can be deleted.
    jdbcTemplate.update(
        "DELETE FROM transactions WHERE user_id = (SELECT id FROM users WHERE email = ?)",
        DEMO_EMAIL);
    jdbcTemplate.update(
        "DELETE FROM holdings WHERE user_id = (SELECT id FROM users WHERE email = ?)", DEMO_EMAIL);
    jdbcTemplate.update("DELETE FROM users WHERE email = ?", DEMO_EMAIL);

    mockMvc
        .perform(post("/auth/demo-login").header("X-Forwarded-For", "203.0.113.31"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.detail").value("Demo user not found"));
  }

  @Test
  void demoLogin_calledRepeatedly_neverTriggersFailedAttemptLockoutTracking() throws Exception {
    // Stays under RateLimitGroup.DEMO_LOGIN's 5/hour cap - rate limiting itself is covered by
    // RateLimitFilterTest, this is only checking LoginService's lockout tracking isn't touched.
    for (int i = 0; i < 3; i++) {
      mockMvc
          .perform(post("/auth/demo-login").header("X-Forwarded-For", "203.0.113.32"))
          .andExpect(status().isOk());
    }

    User demoUser = userRepository.findByEmail(DEMO_EMAIL).orElseThrow();
    assertThat(demoUser.getFailedAttempts()).isZero();
    assertThat(demoUser.getLockedUntil()).isNull();
  }
}
