package com.toptrader.backend.market;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class MarketDataConfig {
    @Value("${toptrader.finnhub.base-url:https://finnhub.io/api/v1}")
    private String finnhubBaseUrl;

    @Value("${toptrader.finnhub.api-key}")
    private String finnhubApiKey;

    @Bean
    public RestClient restClient(){
        return RestClient.builder()
                .baseUrl(finnhubBaseUrl)
                .requestInterceptor(new FinnhubTokenInterceptor(finnhubApiKey))
                .build();
    }

}
