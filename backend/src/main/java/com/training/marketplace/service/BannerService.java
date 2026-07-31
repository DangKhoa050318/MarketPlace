package com.training.marketplace.service;

import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.CreateBannerRequest;
import com.training.marketplace.dto.request.UpdateBannerRequest;
import com.training.marketplace.dto.response.BannerResponse;
import com.training.marketplace.enums.PublishStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BannerService {

    // --- Admin ---
    BannerResponse create(CreateBannerRequest request);

    BannerResponse update(Long id, UpdateBannerRequest request);

    BannerResponse getById(Long id);

    PageResponse<BannerResponse> list(PublishStatus status, String position, Pageable pageable);

    void delete(Long id); // soft delete (active=false)

    BannerResponse publish(Long id);

    BannerResponse unpublish(Long id);

    // --- Storefront ---
    /** Banners effective right now in a slot (PUBLISHED + within schedule), ordered (B-405). */
    List<BannerResponse> listEffectiveByPosition(String position);
}
