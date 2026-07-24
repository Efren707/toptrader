package com.toptrader.backend.trading;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TradeResult(
        Transaction transaction,
        BigDecimal cashBalance,
        Holding holding) {
}
