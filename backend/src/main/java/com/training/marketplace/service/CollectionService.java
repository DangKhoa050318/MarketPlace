package com.training.marketplace.service;

import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.AddCollectionItemRequest;
import com.training.marketplace.dto.request.CreateCollectionRequest;
import com.training.marketplace.dto.request.ReorderCollectionItemsRequest;
import com.training.marketplace.dto.request.UpdateCollectionRequest;
import com.training.marketplace.dto.response.CollectionResponse;
import com.training.marketplace.enums.PublishStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CollectionService {

    // --- Admin ---
    CollectionResponse create(CreateCollectionRequest request);

    CollectionResponse update(Long id, UpdateCollectionRequest request);

    CollectionResponse getById(Long id);

    PageResponse<CollectionResponse> list(PublishStatus status, String q, Pageable pageable);

    void delete(Long id); // soft delete (active=false)

    CollectionResponse publish(Long id);

    CollectionResponse unpublish(Long id);

    CollectionResponse addItem(Long collectionId, AddCollectionItemRequest request);

    void removeItem(Long collectionId, Long productId);

    /** Rewrites every item's display_order in one transaction from the given full ordering (B-406). */
    CollectionResponse reorder(Long collectionId, ReorderCollectionItemsRequest request);

    // --- Storefront ---
    CollectionResponse getPublishedBySlug(String slug);

    List<CollectionResponse> listPublished();
}
