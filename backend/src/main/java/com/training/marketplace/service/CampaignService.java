package com.training.marketplace.service;

import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.CreateCampaignRequest;
import com.training.marketplace.dto.request.UpdateCampaignRequest;
import com.training.marketplace.dto.response.CampaignResponse;
import com.training.marketplace.enums.CampaignStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CampaignService {

    // --- Admin ---
    CampaignResponse create(CreateCampaignRequest request);

    CampaignResponse update(Long id, UpdateCampaignRequest request);

    CampaignResponse getById(Long id);

    PageResponse<CampaignResponse> list(CampaignStatus status, String q, Pageable pageable);

    void delete(Long id); // soft delete (active=false)

    CampaignResponse publish(Long id);

    CampaignResponse archive(Long id);

    // --- Storefront ---
    /** Campaigns effective right now (PUBLISHED + within schedule), computed server-side (B-405). */
    List<CampaignResponse> listEffective();
}
