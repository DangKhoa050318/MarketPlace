package com.training.marketplace.service;

import com.training.marketplace.dto.response.ProductResponse;
import com.training.marketplace.entity.Product;
import com.training.marketplace.entity.WishlistItem;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.mapper.ProductMapper;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.WishlistItemRepository;
import com.training.marketplace.service.impl.WishlistServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock private WishlistItemRepository wishlistItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductMapper productMapper;

    @InjectMocks private WishlistServiceImpl wishlistService;

    @Test
    void add_existingWishlistItem_returnsExistingItemWithoutDuplicateSave() {
        Product product = buildProduct(10L, "phone", "Phone");
        WishlistItem item = buildItem(100L, 7L, 10L);
        ProductResponse productResponse = buildProductResponse(product);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(wishlistItemRepository.findByUserIdAndProductId(7L, 10L)).thenReturn(Optional.of(item));
        when(productMapper.toResponse(product)).thenReturn(productResponse);

        var result = wishlistService.add(7L, 10L);

        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.userId()).isEqualTo(7L);
        assertThat(result.product().id()).isEqualTo(10L);
        verify(wishlistItemRepository, never()).save(any());
    }

    @Test
    void add_newWishlistItem_savesItem() {
        Product product = buildProduct(10L, "phone", "Phone");
        WishlistItem saved = buildItem(100L, 7L, 10L);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(wishlistItemRepository.findByUserIdAndProductId(7L, 10L)).thenReturn(Optional.empty());
        when(wishlistItemRepository.save(any(WishlistItem.class))).thenReturn(saved);
        when(productMapper.toResponse(product)).thenReturn(buildProductResponse(product));

        var result = wishlistService.add(7L, 10L);

        assertThat(result.id()).isEqualTo(100L);
        verify(wishlistItemRepository).save(any(WishlistItem.class));
    }

    @Test
    void add_inactiveOrMissingProduct_throwsNotFound() {
        Product inactive = buildProduct(10L, "phone", "Phone");
        inactive.setActive(false);
        when(productRepository.findById(10L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> wishlistService.add(7L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(wishlistItemRepository, never()).save(any());
    }

    @Test
    void status_existingItem_returnsWishlistedTrue() {
        when(wishlistItemRepository.existsByUserIdAndProductId(7L, 10L)).thenReturn(true);

        var result = wishlistService.status(7L, 10L);

        assertThat(result.productId()).isEqualTo(10L);
        assertThat(result.wishlisted()).isTrue();
    }

    @Test
    void remove_deletesByUserAndProduct() {
        wishlistService.remove(7L, 10L);

        verify(wishlistItemRepository).deleteByUserIdAndProductId(7L, 10L);
    }

    private Product buildProduct(Long id, String slug, String name) {
        Product product = Product.builder()
                .slug(slug)
                .name(name)
                .description("Desc")
                .categoryId(1L)
                .unit("PCS")
                .imageUrl("http://example.com/image.png")
                .active(true)
                .build();
        product.setId(id);
        return product;
    }

    private ProductResponse buildProductResponse(Product product) {
        return new ProductResponse(product.getId(), product.getSlug(), product.getName(), product.getDescription(),
                product.getCategoryId(), product.getUnit(), product.getImageUrl(), product.isActive(),
                LocalDateTime.now(), LocalDateTime.now(), null);
    }

    private WishlistItem buildItem(Long id, Long userId, Long productId) {
        WishlistItem item = WishlistItem.builder()
                .userId(userId)
                .productId(productId)
                .build();
        item.setId(id);
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        return item;
    }
}
