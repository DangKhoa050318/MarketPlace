package com.training.marketplace.service.impl;

import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.AddCollectionItemRequest;
import com.training.marketplace.dto.request.CreateCollectionRequest;
import com.training.marketplace.dto.request.ReorderCollectionItemsRequest;
import com.training.marketplace.dto.request.UpdateCollectionRequest;
import com.training.marketplace.dto.response.CollectionItemResponse;
import com.training.marketplace.dto.response.CollectionResponse;
import com.training.marketplace.entity.Product;
import com.training.marketplace.entity.ProductCollection;
import com.training.marketplace.entity.ProductCollectionItem;
import com.training.marketplace.enums.PublishStatus;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.DuplicateResourceException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.mapper.CollectionMapper;
import com.training.marketplace.repository.ProductCollectionItemRepository;
import com.training.marketplace.repository.ProductCollectionRepository;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.service.CollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionServiceImpl implements CollectionService {

    private final ProductCollectionRepository collectionRepository;
    private final ProductCollectionItemRepository itemRepository;
    private final ProductRepository productRepository;
    private final CollectionMapper collectionMapper;

    @Override
    @Transactional
    public CollectionResponse create(CreateCollectionRequest request) {
        if (collectionRepository.existsBySlug(request.slug())) {
            throw new DuplicateResourceException("Collection", "slug", request.slug());
        }
        ProductCollection collection = collectionMapper.toEntity(request); // status=DRAFT, active=true
        ProductCollection saved = collectionRepository.save(collection);
        log.info("Collection created: id={}, slug={}", saved.getId(), saved.getSlug());
        return summaryResponse(saved);
    }

    @Override
    @Transactional
    public CollectionResponse update(Long id, UpdateCollectionRequest request) {
        ProductCollection collection = findOr404(id);
        if (!collection.getSlug().equals(request.slug()) && collectionRepository.existsBySlug(request.slug())) {
            throw new DuplicateResourceException("Collection", "slug", request.slug());
        }
        collectionMapper.updateEntity(request, collection);
        return detailResponse(collectionRepository.save(collection));
    }

    @Override
    @Transactional(readOnly = true)
    public CollectionResponse getById(Long id) {
        return detailResponse(findOr404(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CollectionResponse> list(PublishStatus status, String q, Pageable pageable) {
        return PageResponse.from(collectionRepository.search(status, q, pageable), this::summaryResponse);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ProductCollection collection = findOr404(id);
        collection.setActive(false);
        collectionRepository.save(collection);
        log.info("Collection soft-deleted: id={}", id);
    }

    @Override
    @Transactional
    public CollectionResponse publish(Long id) {
        ProductCollection collection = findOr404(id);
        collection.setStatus(PublishStatus.PUBLISHED);
        return detailResponse(collectionRepository.save(collection));
    }

    @Override
    @Transactional
    public CollectionResponse unpublish(Long id) {
        ProductCollection collection = findOr404(id);
        collection.setStatus(PublishStatus.DRAFT);
        return detailResponse(collectionRepository.save(collection));
    }

    @Override
    @Transactional
    public CollectionResponse addItem(Long collectionId, AddCollectionItemRequest request) {
        ProductCollection collection = findOr404(collectionId);
        Long productId = request.productId();
        if (!productRepository.existsById(productId)) {
            throw new BadRequestException("Product not found with id: " + productId);
        }
        if (itemRepository.existsByCollectionIdAndProductId(collectionId, productId)) {
            throw new DuplicateResourceException("Collection item", "productId", productId);
        }
        int nextOrder = itemRepository.findMaxDisplayOrder(collectionId) + 1;
        itemRepository.save(ProductCollectionItem.builder()
                .collectionId(collectionId)
                .productId(productId)
                .displayOrder(nextOrder)
                .build());
        return detailResponse(collection);
    }

    @Override
    @Transactional
    public void removeItem(Long collectionId, Long productId) {
        ProductCollectionItem item = itemRepository.findByCollectionIdAndProductId(collectionId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection item", "productId", productId));
        itemRepository.delete(item);
    }

    @Override
    @Transactional
    public CollectionResponse reorder(Long collectionId, ReorderCollectionItemsRequest request) {
        // Lock the collection row so concurrent reorders serialize (B-406).
        ProductCollection collection = collectionRepository.findByIdForUpdate(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", collectionId));

        List<ProductCollectionItem> items = itemRepository.findByCollectionIdOrderByDisplayOrderAsc(collectionId);
        Map<Long, ProductCollectionItem> byProduct = items.stream()
                .collect(Collectors.toMap(ProductCollectionItem::getProductId, Function.identity()));

        List<Long> requested = request.productIds();
        if (requested.size() != items.size() || !byProduct.keySet().equals(new HashSet<>(requested))) {
            throw new BadRequestException(
                    "productIds must be exactly the collection's current products (no additions/removals)");
        }

        // Rewrite every position; the display_order UNIQUE constraint is DEFERRABLE so intermediate
        // duplicates are tolerated and checked at commit.
        int order = 0;
        for (Long productId : requested) {
            byProduct.get(productId).setDisplayOrder(order++);
        }
        itemRepository.saveAll(byProduct.values());
        log.info("Collection reordered: id={}, items={}", collectionId, items.size());
        return detailResponse(collection);
    }

    @Override
    @Transactional(readOnly = true)
    public CollectionResponse getPublishedBySlug(String slug) {
        ProductCollection collection = collectionRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", "slug", slug));
        if (collection.getStatus() != PublishStatus.PUBLISHED || !Boolean.TRUE.equals(collection.getActive())) {
            throw new ResourceNotFoundException("Collection", "slug", slug);
        }
        return detailResponse(collection);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CollectionResponse> listPublished() {
        return collectionRepository.search(PublishStatus.PUBLISHED, null, Pageable.unpaged()).getContent().stream()
                .filter(c -> Boolean.TRUE.equals(c.getActive()))
                .map(this::summaryResponse)
                .toList();
    }

    // ---------------------------------------------------------------- helpers

    private ProductCollection findOr404(Long id) {
        return collectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", id));
    }

    /** Collection without its items (list views). */
    private CollectionResponse summaryResponse(ProductCollection collection) {
        return collectionMapper.toResponse(collection, List.of());
    }

    /** Collection with its ordered items, enriched with product name/slug/image. */
    private CollectionResponse detailResponse(ProductCollection collection) {
        List<ProductCollectionItem> items =
                itemRepository.findByCollectionIdOrderByDisplayOrderAsc(collection.getId());
        List<Long> productIds = items.stream().map(ProductCollectionItem::getProductId).toList();
        Map<Long, Product> products = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<CollectionItemResponse> itemResponses = items.stream()
                .map(item -> {
                    Product p = products.get(item.getProductId());
                    return new CollectionItemResponse(
                            item.getProductId(),
                            item.getDisplayOrder(),
                            p != null ? p.getName() : null,
                            p != null ? p.getSlug() : null,
                            p != null ? p.getImageUrl() : null);
                })
                .toList();
        return collectionMapper.toResponse(collection, itemResponses);
    }
}
