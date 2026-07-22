package com.toptrader.backend.market;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FinnhubQuoteResponse(
        @JsonProperty("c") BigDecimal currentPrice,
        @JsonProperty("t") long timestamp) {
}
