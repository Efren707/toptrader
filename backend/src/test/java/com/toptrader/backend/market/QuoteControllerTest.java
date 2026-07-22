package com.toptrader.backend.market;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClientException;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class QuoteControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private FinnhubClient finnhubClient;

  @Test
  void getQuote_withValidTicker_returns200WithQuoteData() throws Exception {
    when(finnhubClient.fetchQuote("AAPL"))
        .thenReturn(new FinnhubQuoteResponse(new BigDecimal("210.50"), 1721500000L));
    when(finnhubClient.fetchProfile("AAPL"))
        .thenReturn(new FinnhubCompanyProfileResponse("Apple Inc"));

    mockMvc
        .perform(get("/quotes/AAPL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ticker").value("AAPL"))
        .andExpect(jsonPath("$.companyName").value("Apple Inc"))
        .andExpect(jsonPath("$.price").value(210.50))
        .andExpect(jsonPath("$.asOf").exists());
  }

  @Test
  void getQuote_withZeroPrice_returns404() throws Exception {
    when(finnhubClient.fetchQuote("ZZZZ"))
        .thenReturn(new FinnhubQuoteResponse(BigDecimal.ZERO, 1721500000L));
    when(finnhubClient.fetchProfile("ZZZZ"))
        .thenReturn(new FinnhubCompanyProfileResponse("irrelevant"));

    mockMvc
        .perform(get("/quotes/ZZZZ"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Unknown ticker: ZZZZ"));
  }

  @Test
  void getQuote_withMissingCompanyName_returns404() throws Exception {
    when(finnhubClient.fetchQuote("ZZZZ"))
        .thenReturn(new FinnhubQuoteResponse(new BigDecimal("210.50"), 1721500000L));
    when(finnhubClient.fetchProfile("ZZZZ")).thenReturn(new FinnhubCompanyProfileResponse(null));

    mockMvc
        .perform(get("/quotes/ZZZZ"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Unknown ticker: ZZZZ"));
  }

  @Test
  void getQuote_whenQuoteCallFails_returns502() throws Exception {
    when(finnhubClient.fetchQuote("AAPL")).thenThrow(new RestClientException("connection refused"));

    mockMvc.perform(get("/quotes/AAPL")).andExpect(status().isBadGateway());
  }

  @Test
  void getQuote_whenProfileCallFails_returns502() throws Exception {
    when(finnhubClient.fetchQuote("AAPL"))
        .thenReturn(new FinnhubQuoteResponse(new BigDecimal("210.50"), 1721500000L));
    when(finnhubClient.fetchProfile("AAPL")).thenThrow(new RestClientException("timeout"));

    mockMvc.perform(get("/quotes/AAPL")).andExpect(status().isBadGateway());
  }

  @Test
  void getQuote_withoutSession_returns401() throws Exception {
    mockMvc.perform(get("/quotes/AAPL").with(anonymous())).andExpect(status().isUnauthorized());
  }
}
