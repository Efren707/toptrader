package com.toptrader.backend.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QuoteService {
  private final FinnhubClient finnhubClient;

  public QuoteService(FinnhubClient finnhubClient) {
    this.finnhubClient = finnhubClient;
  }

  public Quote getQuote(String ticker) {
    ticker = ticker.toUpperCase(Locale.ROOT);
    FinnhubQuoteResponse quoteResponse;
    FinnhubCompanyProfileResponse profileResponse;

    try {
      quoteResponse = finnhubClient.fetchQuote(ticker);
    } catch (RestClientException e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Provider error fetching quote for " + ticker);
    }

    try {
      profileResponse = finnhubClient.fetchProfile(ticker);
    } catch (RestClientException e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Provider error fetching quote for " + ticker);
    }

    if (quoteResponse.currentPrice().compareTo(BigDecimal.ZERO) == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown ticker: " + ticker);
    }

    if (profileResponse.name() == null || profileResponse.name().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown ticker: " + ticker);
    }

    return new Quote(
        ticker,
        profileResponse.name(),
        quoteResponse.currentPrice(),
        Instant.ofEpochSecond(quoteResponse.timestamp()));
  }
}
