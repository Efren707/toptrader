package com.toptrader.backend.market;

import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.util.UriComponentsBuilder;

/** Appends Finnhub's required {@code token} query param to every outgoing request (ADR 0003). */
public class FinnhubTokenInterceptor implements ClientHttpRequestInterceptor {

  private final String apiKey;

  public FinnhubTokenInterceptor(String apiKey) {
    this.apiKey = apiKey;
  }

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    HttpRequest authorizedRequest =
        new HttpRequestWrapper(request) {
          @Override
          public URI getURI() {
            return UriComponentsBuilder.fromUri(super.getURI())
                .queryParam("token", apiKey)
                .build()
                .toUri();
          }
        };
    return execution.execute(authorizedRequest, body);
  }
}
