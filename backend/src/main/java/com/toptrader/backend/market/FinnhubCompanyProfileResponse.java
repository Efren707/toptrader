package com.toptrader.backend.market;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FinnhubCompanyProfileResponse(String name) {
}
