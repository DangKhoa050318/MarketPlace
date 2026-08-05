package com.training.marketplace.service.impl;

import com.training.marketplace.service.CurrencyConversionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
public class CurrencyConversionServiceImpl implements CurrencyConversionService {

    @Value("${marketplace.paygate.usd-to-vnd-rate:25000}")
    private BigDecimal usdToVndRate;

    @Override
    public BigDecimal convertUsdToVnd(BigDecimal usdAmount) {
        if (usdAmount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal vndAmount = usdAmount.multiply(usdToVndRate).setScale(0, RoundingMode.HALF_UP);
        log.debug("Converted USD amount {} to VND amount {} at rate {}", usdAmount, vndAmount, usdToVndRate);
        return vndAmount;
    }
}
