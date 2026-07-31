package com.training.marketplace.service.impl;

import com.training.marketplace.dto.response.FunnelStepResponse;
import com.training.marketplace.dto.response.FunnelSummaryResponse;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.repository.AnalyticsEventRepository;
import com.training.marketplace.service.FunnelAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FunnelAnalyticsServiceImpl implements FunnelAnalyticsService {

    private final AnalyticsEventRepository analyticsEventRepository;

    @Override
    @Transactional(readOnly = true)
    public FunnelSummaryResponse summarize(
            Instant from,
            Instant to,
            Long categoryId,
            Long productId,
            String campaign,
            String placement,
            String deviceType) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new BadRequestException("A valid from/to range is required");
        }
        var counts = analyticsEventRepository.summarizeFunnel(
                from, to, categoryId, productId, blankToNull(campaign),
                blankToNull(placement), blankToNull(deviceType));
        long productViews = counts == null ? 0 : counts.getProductViews();
        long addToCarts = counts == null ? 0 : counts.getAddToCarts();
        long beginCheckouts = counts == null ? 0 : counts.getBeginCheckouts();
        long orderCreated = counts == null ? 0 : counts.getOrderCreated();

        return new FunnelSummaryResponse(
                from,
                to,
                categoryId,
                productId,
                blankToNull(campaign),
                blankToNull(deviceType),
                List.of(
                        step("PRODUCT_VIEW", productViews, productViews),
                        step("ADD_TO_CART", addToCarts, productViews),
                        step("BEGIN_CHECKOUT", beginCheckouts, addToCarts),
                        step("ORDER_CREATED", orderCreated, beginCheckouts)));
    }

    private FunnelStepResponse step(String name, long count, long previousCount) {
        double conversion = previousCount == 0 ? 0 : (double) count / previousCount;
        return new FunnelStepResponse(name, count, conversion, previousCount == 0 ? 0 : 1 - conversion);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
