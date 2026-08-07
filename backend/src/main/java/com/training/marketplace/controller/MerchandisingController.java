package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.dto.request.RecordMerchandisingEventRequest;
import com.training.marketplace.dto.response.BannerResponse;
import com.training.marketplace.dto.response.CampaignResponse;
import com.training.marketplace.dto.response.CollectionResponse;
import com.training.marketplace.entity.User;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.service.BannerService;
import com.training.marketplace.service.CampaignService;
import com.training.marketplace.service.CollectionService;
import com.training.marketplace.service.MerchandisingEventService;
import com.training.marketplace.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public storefront merchandising: effective campaigns/collections/banners (visibility computed
 * server-side, B-405) and impression/click ingestion (B-407). All endpoints are permitAll; the
 * event recorder resolves the user id only when the caller happens to be authenticated.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Merchandising", description = "Storefront campaigns/collections/banners and event tracking")
public class MerchandisingController {

    private final CampaignService campaignService;
    private final CollectionService collectionService;
    private final BannerService bannerService;
    private final MerchandisingEventService merchandisingEventService;
    private final UserService userService;

    @GetMapping("/campaigns")
    @Operation(summary = "Currently effective campaigns")
    public ApiResponse<List<CampaignResponse>> effectiveCampaigns() {
        return ApiResponse.success(campaignService.listEffective());
    }

    @GetMapping("/collections")
    @Operation(summary = "Published collections")
    public ApiResponse<List<CollectionResponse>> publishedCollections() {
        return ApiResponse.success(collectionService.listPublished());
    }

    @GetMapping("/collections/{slug}")
    @Operation(summary = "A published collection with its ordered products")
    public ApiResponse<CollectionResponse> collectionBySlug(@PathVariable String slug) {
        return ApiResponse.success(collectionService.getPublishedBySlug(slug));
    }

    @GetMapping("/banners")
    @Operation(summary = "Currently effective banners in a slot")
    public ApiResponse<List<BannerResponse>> effectiveBanners(@RequestParam String position) {
        return ApiResponse.success(bannerService.listEffectiveByPosition(position));
    }

    @PostMapping("/merchandising/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Record an impression/click (idempotent by eventId)")
    public ApiResponse<Void> recordEvent(Authentication authentication,
                                         @Valid @RequestBody RecordMerchandisingEventRequest request) {
        merchandisingEventService.record(request, optionalUserId(authentication));
        return ApiResponse.<Void>success("Event recorded", null);
    }

    /** User id when authenticated, else null (anonymous storefront visitors still generate events). */
    private Long optionalUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        try {
            User user = userService.getAuthenticatedUser(authentication);
            return user != null ? user.getId() : null;
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }
}

