package com.training.marketplace.service;

import com.training.marketplace.dto.response.ProductResponse;
import com.training.marketplace.entity.Product;
import com.training.marketplace.entity.RecentlyViewedProduct;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.mapper.ProductMapper;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.RecentlyViewedProductRepository;
import com.training.marketplace.service.impl.RecentlyViewedProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecentlyViewedProductServiceTest {

    @Mock private RecentlyViewedProductRepository recentlyViewedRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductMapper productMapper;

    @InjectMocks private RecentlyViewedProductServiceImpl recentlyViewedService;

    @Test
    void record_existingSessionProduct_updatesViewedAtAndTrimsDuplicates() {
        Product product = buildProduct(1L, "phone", "Phone");
        RecentlyViewedProduct existing = buildView(10L, null, "session-1", 1L, LocalDateTime.now().minusDays(1));
        ProductResponse productResponse = buildProductResponse(product);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(recentlyViewedRepository.findBySessionIdAndProductId("session-1", 1L)).thenReturn(Optional.of(existing));
        when(recentlyViewedRepository.save(existing)).thenReturn(existing);
        when(productMapper.toResponse(product)).thenReturn(productResponse);

        var result = recentlyViewedService.record(null, "session-1", 1L);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.product().id()).isEqualTo(1L);
        assertThat(existing.getViewedAt()).isAfter(LocalDateTime.now().minusMinutes(1));
        verify(recentlyViewedRepository).deleteSessionOverflow("session-1", 20);
        verify(recentlyViewedRepository, never()).deleteUserOverflow(any(), eq(20));
    }

    @Test
    void list_userViews_returnsNewestOrderFromRepositoryWithBoundedLimit() {
        Product newest = buildProduct(2L, "tablet", "Tablet");
        Product older = buildProduct(1L, "phone", "Phone");
        RecentlyViewedProduct newestView = buildView(22L, 7L, null, 2L, LocalDateTime.now());
        RecentlyViewedProduct olderView = buildView(21L, 7L, null, 1L, LocalDateTime.now().minusMinutes(5));

        when(recentlyViewedRepository.findRecentByUserId(eq(7L), any(Pageable.class)))
                .thenReturn(List.of(newestView, olderView));
        when(productRepository.findAllById(any())).thenReturn(List.of(newest, older));
        when(productMapper.toResponse(newest)).thenReturn(buildProductResponse(newest));
        when(productMapper.toResponse(older)).thenReturn(buildProductResponse(older));

        var result = recentlyViewedService.list(7L, null, 99);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).product().id()).isEqualTo(2L);
        assertThat(result.get(1).product().id()).isEqualTo(1L);
    }

    @Test
    void list_anonymousWithoutSession_throwsBadRequest() {
        assertThatThrownBy(() -> recentlyViewedService.list(null, " ", 20))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("X-Session-Id");
    }

    @Test
    void record_userProduct_updatesExistingUserViewAndTrimsUserOverflow() {
        Product product = buildProduct(1L, "phone", "Phone");
        RecentlyViewedProduct existing = buildView(11L, 7L, null, 1L, LocalDateTime.now().minusDays(1));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(recentlyViewedRepository.findByUserIdAndProductId(7L, 1L)).thenReturn(Optional.of(existing));
        when(recentlyViewedRepository.save(existing)).thenReturn(existing);
        when(productMapper.toResponse(product)).thenReturn(buildProductResponse(product));

        recentlyViewedService.record(7L, "ignored-session", 1L);

        assertThat(existing.getViewedAt()).isAfter(LocalDateTime.now().minusMinutes(1));
        verify(recentlyViewedRepository).deleteUserOverflow(7L, 20);
        verify(recentlyViewedRepository, never()).deleteSessionOverflow(any(), eq(20));
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

    private RecentlyViewedProduct buildView(Long id, Long userId, String sessionId, Long productId, LocalDateTime viewedAt) {
        RecentlyViewedProduct view = RecentlyViewedProduct.builder()
                .userId(userId)
                .sessionId(sessionId)
                .productId(productId)
                .viewedAt(viewedAt)
                .build();
        view.setId(id);
        return view;
    }
}
