package com.toptrader.backend.market;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class FinnhubClient {

  private final RestClient restClient;

  public FinnhubClient(RestClient restClient) {
    this.restClient = restClient;
  }

  public FinnhubQuoteResponse fetchQuote(String ticker) {
    return restClient
        .get()
        .uri("/quote?symbol={ticker}", ticker)
        .retrieve()
        .body(FinnhubQuoteResponse.class);
  }

  public FinnhubCompanyProfileResponse fetchProfile(String ticker) {
    return restClient
        .get()
        .uri("/stock/profile2?symbol={ticker}", ticker)
        .retrieve()
        .body(FinnhubCompanyProfileResponse.class);
  }
}
