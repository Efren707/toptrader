package com.toptrader.backend.trading;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TradeRequest(@NotBlank String ticker, @Positive @NotNull Integer quantity) {}
