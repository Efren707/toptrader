package com.toptrader.backend.trading;

import java.math.BigDecimal;

public record HoldingResponse(
    String ticker,
    Integer quantity,
    BigDecimal averageCostBasis,
    BigDecimal currentPrice,
    BigDecimal marketValue,
    BigDecimal unrealizedGainLoss) {}
