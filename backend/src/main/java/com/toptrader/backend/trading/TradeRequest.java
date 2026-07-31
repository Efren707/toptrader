package com.toptrader.backend.trading;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record TradeRequest(
    @Pattern(regexp = "^[A-Z]{1,5}$", message = "Invalid stock ticker") @NotBlank String ticker,
    @Positive @NotNull Integer quantity) {}
