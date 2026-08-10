package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.AddCollectionItemRequest;
import com.training.marketplace.dto.request.CreateCollectionRequest;
import com.training.marketplace.dto.request.ReorderCollectionItemsRequest;
import com.training.marketplace.dto.request.UpdateCollectionRequest;
import com.training.marketplace.dto.response.CollectionResponse;
import com.training.marketplace.enums.PublishStatus;
import com.training.marketplace.service.AuditService;
import com.training.marketplace.service.CollectionService;
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

/** Product collection management (ADMIN), including items and drag-drop reordering (B-406). */
@RestController
@RequestMapping("/api/v1/admin/collections")
@RequiredArgsConstructor
@Tag(name = "Admin Collections", description = "Product collection management (ADMIN)")
public class AdminCollectionController {

    private final CollectionService collectionService;
    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "List collections with filters (ADMIN)")
    public ApiResponse<PageResponse<CollectionResponse>> list(
            @RequestParam(required = false) PublishStatus status,
            @RequestParam(required = false) String q,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(collectionService.list(status, q, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a collection with its items (ADMIN)")
    public ApiResponse<CollectionResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(collectionService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a collection (ADMIN)")
    public ApiResponse<CollectionResponse> create(Authentication authentication,
                                                  @Valid @RequestBody CreateCollectionRequest request) {
        CollectionResponse created = collectionService.create(request);
        auditService.record(actor(authentication), "COLLECTION_CREATE", "ProductCollection", created.id(),
                "slug=" + created.slug());
        return ApiResponse.success("Collection created", created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a collection (ADMIN)")
    public ApiResponse<CollectionResponse> update(Authentication authentication, @PathVariable Long id,
                                                  @Valid @RequestBody UpdateCollectionRequest request) {
        CollectionResponse updated = collectionService.update(id, request);
        auditService.record(actor(authentication), "COLLECTION_UPDATE", "ProductCollection", id,
                "slug=" + updated.slug());
        return ApiResponse.success("Collection updated", updated);
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish a collection (ADMIN)")
    public ApiResponse<CollectionResponse> publish(Authentication authentication, @PathVariable Long id) {
        CollectionResponse result = collectionService.publish(id);
        auditService.record(actor(authentication), "COLLECTION_PUBLISH", "ProductCollection", id, null);
        return ApiResponse.success("Collection published", result);
    }

    @PostMapping("/{id}/unpublish")
    @Operation(summary = "Unpublish a collection (ADMIN)")
    public ApiResponse<CollectionResponse> unpublish(Authentication authentication, @PathVariable Long id) {
        CollectionResponse result = collectionService.unpublish(id);
        auditService.record(actor(authentication), "COLLECTION_UNPUBLISH", "ProductCollection", id, null);
        return ApiResponse.success("Collection unpublished", result);
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore a soft-deleted collection (ADMIN)")
    public ApiResponse<CollectionResponse> restore(Authentication authentication, @PathVariable Long id) {
        CollectionResponse result = collectionService.restore(id);
        auditService.record(actor(authentication), "COLLECTION_RESTORE", "ProductCollection", id, "active=true");
        return ApiResponse.success("Collection restored", result);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a collection: soft (active=false) by default, permanent with ?hard=true (ADMIN)")
    public void delete(Authentication authentication, @PathVariable Long id,
                       @RequestParam(defaultValue = "false") boolean hard) {
        if (hard) {
            collectionService.hardDelete(id);
            auditService.record(actor(authentication), "COLLECTION_HARD_DELETE", "ProductCollection", id,
                    "permanent delete");
        } else {
            collectionService.delete(id);
            auditService.record(actor(authentication), "COLLECTION_DELETE", "ProductCollection", id,
                    "soft delete (active=false)");
        }
    }

    // --- items ---

    @PostMapping("/{id}/items")
    @Operation(summary = "Add a product to a collection (ADMIN)")
    public ApiResponse<CollectionResponse> addItem(Authentication authentication, @PathVariable Long id,
                                                   @Valid @RequestBody AddCollectionItemRequest request) {
        CollectionResponse result = collectionService.addItem(id, request);
        auditService.record(actor(authentication), "COLLECTION_ADD_ITEM", "ProductCollection", id,
                "productId=" + request.productId());
        return ApiResponse.success("Item added", result);
    }

    @DeleteMapping("/{id}/items/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a product from a collection (ADMIN)")
    public void removeItem(Authentication authentication, @PathVariable Long id, @PathVariable Long productId) {
        collectionService.removeItem(id, productId);
        auditService.record(actor(authentication), "COLLECTION_REMOVE_ITEM", "ProductCollection", id,
                "productId=" + productId);
    }

    @PutMapping("/{id}/items/order")
    @Operation(summary = "Reorder a collection's items in one transaction (ADMIN, B-406)")
    public ApiResponse<CollectionResponse> reorder(Authentication authentication, @PathVariable Long id,
                                                   @Valid @RequestBody ReorderCollectionItemsRequest request) {
        CollectionResponse result = collectionService.reorder(id, request);
        auditService.record(actor(authentication), "COLLECTION_REORDER", "ProductCollection", id,
                "items=" + request.productIds().size());
        return ApiResponse.success("Collection reordered", result);
    }

    private String actor(Authentication authentication) {
        return authentication != null ? authentication.getName() : null;
    }
}
