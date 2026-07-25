package com.toptrader.backend.trading;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
    Long id,
    String ticker,
    Transaction.Side side,
    Integer quantity,
    BigDecimal pricePerShare,
    BigDecimal totalAmount,
    LocalDateTime executedAt) {}
