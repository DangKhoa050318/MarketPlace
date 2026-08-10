package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.CreateCampaignRequest;
import com.training.marketplace.dto.request.UpdateCampaignRequest;
import com.training.marketplace.dto.response.CampaignResponse;
import com.training.marketplace.enums.CampaignStatus;
import com.training.marketplace.service.AuditService;
import com.training.marketplace.service.CampaignService;
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

/** Campaign management (ADMIN — enforced by SecurityConfig via /api/v1/admin/**). */
@RestController
@RequestMapping("/api/v1/admin/campaigns")
@RequiredArgsConstructor
@Tag(name = "Admin Campaigns", description = "Campaign management (ADMIN)")
public class AdminCampaignController {

    private final CampaignService campaignService;
    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "List campaigns with filters (ADMIN)")
    public ApiResponse<PageResponse<CampaignResponse>> list(
            @RequestParam(required = false) CampaignStatus status,
            @RequestParam(required = false) String q,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(campaignService.list(status, q, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a campaign by id (ADMIN)")
    public ApiResponse<CampaignResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(campaignService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a campaign (ADMIN)")
    public ApiResponse<CampaignResponse> create(Authentication authentication,
                                                @Valid @RequestBody CreateCampaignRequest request) {
        CampaignResponse created = campaignService.create(request);
        auditService.record(actor(authentication), "CAMPAIGN_CREATE", "Campaign", created.id(),
                "name=" + created.name());
        return ApiResponse.success("Campaign created", created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a campaign (ADMIN)")
    public ApiResponse<CampaignResponse> update(Authentication authentication, @PathVariable Long id,
                                                @Valid @RequestBody UpdateCampaignRequest request) {
        CampaignResponse updated = campaignService.update(id, request);
        auditService.record(actor(authentication), "CAMPAIGN_UPDATE", "Campaign", id, "name=" + updated.name());
        return ApiResponse.success("Campaign updated", updated);
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish a campaign (ADMIN)")
    public ApiResponse<CampaignResponse> publish(Authentication authentication, @PathVariable Long id) {
        CampaignResponse result = campaignService.publish(id);
        auditService.record(actor(authentication), "CAMPAIGN_PUBLISH", "Campaign", id, null);
        return ApiResponse.success("Campaign published", result);
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive a campaign (ADMIN)")
    public ApiResponse<CampaignResponse> archive(Authentication authentication, @PathVariable Long id) {
        CampaignResponse result = campaignService.archive(id);
        auditService.record(actor(authentication), "CAMPAIGN_ARCHIVE", "Campaign", id, null);
        return ApiResponse.success("Campaign archived", result);
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore a soft-deleted campaign (ADMIN)")
    public ApiResponse<CampaignResponse> restore(Authentication authentication, @PathVariable Long id) {
        CampaignResponse result = campaignService.restore(id);
        auditService.record(actor(authentication), "CAMPAIGN_RESTORE", "Campaign", id, "active=true");
        return ApiResponse.success("Campaign restored", result);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a campaign: soft (active=false) by default, permanent with ?hard=true (ADMIN)")
    public void delete(Authentication authentication, @PathVariable Long id,
                       @RequestParam(defaultValue = "false") boolean hard) {
        if (hard) {
            campaignService.hardDelete(id);
            auditService.record(actor(authentication), "CAMPAIGN_HARD_DELETE", "Campaign", id, "permanent delete");
        } else {
            campaignService.delete(id);
            auditService.record(actor(authentication), "CAMPAIGN_DELETE", "Campaign", id, "soft delete (active=false)");
        }
    }

    private String actor(Authentication authentication) {
        return authentication != null ? authentication.getName() : null;
    }
}
