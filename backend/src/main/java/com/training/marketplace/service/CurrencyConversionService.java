package com.training.marketplace.service;

import java.math.BigDecimal;

public interface CurrencyConversionService {
    BigDecimal convertUsdToVnd(BigDecimal usdAmount);
}
