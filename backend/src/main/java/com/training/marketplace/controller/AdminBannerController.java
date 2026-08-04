package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.CreateBannerRequest;
import com.training.marketplace.dto.request.UpdateBannerRequest;
import com.training.marketplace.dto.response.BannerResponse;
import com.training.marketplace.enums.PublishStatus;
import com.training.marketplace.service.AuditService;
import com.training.marketplace.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Merchandising banner management (ADMIN). */
@RestController
@RequestMapping("/api/v1/admin/banners")
@RequiredArgsConstructor
@Tag(name = "Admin Banners", description = "Merchandising banner management (ADMIN)")
public class AdminBannerController {

    private final BannerService bannerService;
    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "List banners with filters (ADMIN)")
    public ApiResponse<PageResponse<BannerResponse>> list(
            @RequestParam(required = false) PublishStatus status,
            @RequestParam(required = false) String position,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(bannerService.list(status, position, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a banner by id (ADMIN)")
    public ApiResponse<BannerResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(bannerService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a banner (ADMIN)")
    public ApiResponse<BannerResponse> create(Authentication authentication,
                                              @Valid @RequestBody CreateBannerRequest request) {
        BannerResponse created = bannerService.create(request);
        auditService.record(actor(authentication), "BANNER_CREATE", "MerchandisingBanner", created.id(),
                "position=" + created.position());
        return ApiResponse.success("Banner created", created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a banner (ADMIN)")
    public ApiResponse<BannerResponse> update(Authentication authentication, @PathVariable Long id,
                                              @Valid @RequestBody UpdateBannerRequest request) {
        BannerResponse updated = bannerService.update(id, request);
        auditService.record(actor(authentication), "BANNER_UPDATE", "MerchandisingBanner", id,
                "position=" + updated.position());
        return ApiResponse.success("Banner updated", updated);
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish a banner (ADMIN)")
    public ApiResponse<BannerResponse> publish(Authentication authentication, @PathVariable Long id) {
        BannerResponse result = bannerService.publish(id);
        auditService.record(actor(authentication), "BANNER_PUBLISH", "MerchandisingBanner", id, null);
        return ApiResponse.success("Banner published", result);
    }

    @PostMapping("/{id}/unpublish")
    @Operation(summary = "Unpublish a banner (ADMIN)")
    public ApiResponse<BannerResponse> unpublish(Authentication authentication, @PathVariable Long id) {
        BannerResponse result = bannerService.unpublish(id);
        auditService.record(actor(authentication), "BANNER_UNPUBLISH", "MerchandisingBanner", id, null);
        return ApiResponse.success("Banner unpublished", result);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete a banner (ADMIN)")
    public void delete(Authentication authentication, @PathVariable Long id) {
        bannerService.delete(id);
        auditService.record(actor(authentication), "BANNER_DELETE", "MerchandisingBanner", id,
                "soft delete (active=false)");
    }

    private String actor(Authentication authentication) {
        return authentication != null ? authentication.getName() : null;
    }
}
