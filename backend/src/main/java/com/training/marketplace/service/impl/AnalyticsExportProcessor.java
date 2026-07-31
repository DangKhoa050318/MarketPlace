package com.training.marketplace.service.impl;

import com.training.marketplace.analytics.AnalyticsExportStatus;
import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.response.AnalyticsOverviewResponse;
import com.training.marketplace.dto.response.ProductPerformanceResponse;
import com.training.marketplace.dto.response.PromotionRecommendationPerformanceResponse;
import com.training.marketplace.entity.AnalyticsExportJob;
import com.training.marketplace.repository.AnalyticsExportJobRepository;
import com.training.marketplace.service.AnalyticsDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsExportProcessor {

    private static final Duration DOWNLOAD_TTL = Duration.ofHours(24);

    private final AnalyticsExportJobRepository analyticsExportJobRepository;
    private final AnalyticsDashboardService analyticsDashboardService;

    @Async
    public void process(UUID publicId) {
        AnalyticsExportJob job = analyticsExportJobRepository.findByPublicId(publicId).orElse(null);
        if (job == null) {
            return;
        }
        try {
            job.setStatus(AnalyticsExportStatus.PROCESSING);
            analyticsExportJobRepository.save(job);
            job.setCsvContent(generate(job));
            job.setFileName("analytics-" + job.getExportType().name().toLowerCase()
                    + "-" + job.getPublicId() + ".csv");
            job.setStatus(AnalyticsExportStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            job.setExpiresAt(job.getCompletedAt().plus(DOWNLOAD_TTL));
            job.setErrorMessage(null);
        } catch (RuntimeException exception) {
            job.setStatus(AnalyticsExportStatus.FAILED);
            job.setErrorMessage(truncate(exception.getMessage()));
            job.setCsvContent(null);
        }
        analyticsExportJobRepository.save(job);
    }

    private String generate(AnalyticsExportJob job) {
        return switch (job.getExportType()) {
            case OVERVIEW -> overviewCsv(
                    analyticsDashboardService.overview(AnalyticsExportServiceImpl.filter(job)));
            case PRODUCT_PERFORMANCE -> productPerformanceCsv(job);
            case PROMOTION_RECOMMENDATION -> promotionRecommendationCsv(
                    analyticsDashboardService.promotionRecommendationPerformance(
                            AnalyticsExportServiceImpl.filter(job)));
        };
    }

    private String overviewCsv(AnalyticsOverviewResponse row) {
        StringBuilder csv = new StringBuilder(
                "productViews,addToCarts,beginCheckouts,orders,addToCartRate,"
                        + "checkoutRate,orderConversionRate,returningCustomerRate,lastUpdatedAt\n");
        append(csv, row.productViews(), row.addToCarts(), row.beginCheckouts(), row.orders(),
                row.addToCartRate(), row.checkoutRate(), row.orderConversionRate(),
                row.returningCustomerRate(), row.lastUpdatedAt());
        return csv.toString();
    }

    private String productPerformanceCsv(AnalyticsExportJob job) {
        StringBuilder csv = new StringBuilder(
                "productId,productName,categoryId,productViews,wishlists,addToCarts,"
                        + "orders,averageRating,questionCount,lastUpdatedAt\n");
        int page = 0;
        PageResponse<ProductPerformanceResponse> result;
        do {
            result = analyticsDashboardService.productPerformance(
                    AnalyticsExportServiceImpl.filter(job), page, 100);
            for (ProductPerformanceResponse row : result.getContent()) {
                append(csv, row.productId(), row.productName(), row.categoryId(),
                        row.productViews(), row.wishlists(), row.addToCarts(), row.orders(),
                        row.averageRating(), row.questionCount(), row.lastUpdatedAt());
            }
            page++;
        } while (!result.isLast());
        return csv.toString();
    }

    private String promotionRecommendationCsv(
            List<PromotionRecommendationPerformanceResponse> rows) {
        StringBuilder csv = new StringBuilder(
                "campaign,placement,strategy,impressions,clicks,clickThroughRate,"
                        + "addToCarts,attributedOrders,lastUpdatedAt\n");
        for (PromotionRecommendationPerformanceResponse row : rows) {
            append(csv, row.campaign(), row.placement(), row.strategy(), row.impressions(),
                    row.clicks(), row.clickThroughRate(), row.addToCarts(),
                    row.attributedOrders(), row.lastUpdatedAt());
        }
        return csv.toString();
    }

    private void append(StringBuilder csv, Object... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                csv.append(',');
            }
            csv.append(escape(values[index]));
        }
        csv.append('\n');
    }

    private String escape(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n")
                || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private String truncate(String message) {
        if (message == null || message.isBlank()) {
            return "Analytics export generation failed";
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
