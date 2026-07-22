package com.toptrader.backend.market;

import java.math.BigDecimal;
import java.time.Instant;

public record Quote(String ticker, String companyName, BigDecimal price, Instant asOf) {}
