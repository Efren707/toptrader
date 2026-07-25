package com.toptrader.backend.trading;

import java.math.BigDecimal;

public record TradeResult(
    TransactionResponse transaction, BigDecimal cashBalance, HoldingResponse holding) {}
