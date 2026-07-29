package com.training.marketplace.config;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.response.CategoryResponse;
import com.training.marketplace.dto.response.MovementResponse;
import com.training.marketplace.dto.response.ProductResponse;
import com.training.marketplace.dto.response.ProductSummaryResponse;
import com.training.marketplace.dto.response.RecommendationResponse;
import com.training.marketplace.dto.response.StockLevelResponse;
import com.training.marketplace.dto.response.StockSummaryResponse;
import com.training.marketplace.dto.response.WarehouseResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Concrete documentation-only specializations for generic API envelopes.
 * Runtime serialization continues to use {@link ApiResponse} and {@link PageResponse}.
 */
public final class OpenApiSchemas {

    private OpenApiSchemas() {
    }

    @Schema(name = "ProductResponseEnvelope")
    public static class ProductResponseEnvelope extends ApiResponse<ProductResponse> {
    }

    @Schema(name = "RecommendationResponseEnvelope")
    public static class RecommendationResponseEnvelope
            extends ApiResponse<RecommendationResponse> {
    }

    @Schema(name = "ProductPageResponseEnvelope")
    public static class ProductPageResponseEnvelope
            extends ApiResponse<PageResponse<ProductSummaryResponse>> {
    }

    @Schema(name = "CategoryResponseEnvelope")
    public static class CategoryResponseEnvelope extends ApiResponse<CategoryResponse> {
    }

    @Schema(name = "CategoryPageResponseEnvelope")
    public static class CategoryPageResponseEnvelope
            extends ApiResponse<PageResponse<CategoryResponse>> {
    }

    @Schema(name = "WarehouseResponseEnvelope")
    public static class WarehouseResponseEnvelope extends ApiResponse<WarehouseResponse> {
    }

    @Schema(name = "WarehousePageResponseEnvelope")
    public static class WarehousePageResponseEnvelope
            extends ApiResponse<PageResponse<WarehouseResponse>> {
    }

    @Schema(name = "StockLevelPageResponseEnvelope")
    public static class StockLevelPageResponseEnvelope
            extends ApiResponse<PageResponse<StockLevelResponse>> {
    }

    @Schema(name = "StockSummaryPageResponseEnvelope")
    public static class StockSummaryPageResponseEnvelope
            extends ApiResponse<PageResponse<StockSummaryResponse>> {
    }

    @Schema(name = "MovementResponseEnvelope")
    public static class MovementResponseEnvelope extends ApiResponse<MovementResponse> {
    }

    @Schema(name = "MovementPageResponseEnvelope")
    public static class MovementPageResponseEnvelope
            extends ApiResponse<PageResponse<MovementResponse>> {
    }

    @Schema(name = "ErrorResponseEnvelope", description = "Standard StockPulse error response")
    public static class ErrorResponseEnvelope extends ApiResponse<Object> {
    }
}
