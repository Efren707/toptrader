package com.toptrader.backend.config;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.toptrader.backend.market.FinnhubClient;
import com.toptrader.backend.market.FinnhubCompanyProfileResponse;
import com.toptrader.backend.market.FinnhubQuoteResponse;
import com.toptrader.backend.user.User;
import com.toptrader.backend.user.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/** Verifies ADR 0034's rate-limit thresholds, IP vs. user keying, and the shared trade bucket. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RateLimitFilterTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @MockitoBean private FinnhubClient finnhubClient;

  @Test
  void register_exceedingLimit_returns429WithRetryAfter() throws Exception {
    String sourceIp = "203.0.113.10";

    for (int i = 0; i < 5; i++) {
      mockMvc
          .perform(
              post("/auth/register")
                  .header("X-Forwarded-For", sourceIp)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(registerRequestBody("short")))
          .andExpect(status().isBadRequest());
    }

    mockMvc
        .perform(
            post("/auth/register")
                .header("X-Forwarded-For", sourceIp)
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequestBody("short")))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
        .andExpect(jsonPath("$.detail").value("Rate limit exceeded. Try again later."));
  }

  @Test
  void quote_exceedingLimitForOneUser_doesNotAffectAnotherUser() throws Exception {
    when(finnhubClient.fetchQuote("AAPL"))
        .thenReturn(
            new FinnhubQuoteResponse(new BigDecimal("210.50"), new BigDecimal("5.19"), 1721500000L));
    when(finnhubClient.fetchProfile("AAPL"))
        .thenReturn(new FinnhubCompanyProfileResponse("Apple Inc"));

    seedUser("quotelimit-a@example.com", "quotelimita", "password123");
    MockHttpSession sessionA = loginAndGetSession("quotelimit-a@example.com", "password123");

    for (int i = 0; i < 20; i++) {
      mockMvc.perform(get("/quotes/AAPL").session(sessionA)).andExpect(status().isOk());
    }

    mockMvc
        .perform(get("/quotes/AAPL").session(sessionA))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists(HttpHeaders.RETRY_AFTER));

    seedUser("quotelimit-b@example.com", "quotelimitb", "password123");
    MockHttpSession sessionB = loginAndGetSession("quotelimit-b@example.com", "password123");

    mockMvc.perform(get("/quotes/AAPL").session(sessionB)).andExpect(status().isOk());
  }

  @Test
  void trade_buyAndSellShareOneBucket_returns429After10CombinedRequests() throws Exception {
    seedUser("tradelimit@example.com", "tradelimit", "password123");
    MockHttpSession session = loginAndGetSession("tradelimit@example.com", "password123");

    for (int i = 0; i < 5; i++) {
      mockMvc
          .perform(
              post("/trades/buy")
                  .with(csrf())
                  .session(session)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(tradeRequestBody(0)))
          .andExpect(status().isBadRequest());

      mockMvc
          .perform(
              post("/trades/sell")
                  .with(csrf())
                  .session(session)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(tradeRequestBody(0)))
          .andExpect(status().isBadRequest());
    }

    mockMvc
        .perform(
            post("/trades/buy")
                .with(csrf())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(tradeRequestBody(0)))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
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

  private String registerRequestBody(String password) {
    return """
        {"email":"ratelimit-register@example.com","username":"ratelimitreg","password":"%s"}
        """
        .formatted(password);
  }

  private String loginRequestBody(String email, String password) {
    return """
        {"email":"%s","password":"%s"}
        """
        .formatted(email, password);
  }

  private String tradeRequestBody(int quantity) {
    return """
        {"ticker":"AAPL","quantity":%d}
        """
        .formatted(quantity);
  }
}
