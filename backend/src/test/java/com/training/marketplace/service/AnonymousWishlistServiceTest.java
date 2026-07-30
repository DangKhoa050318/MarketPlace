package com.training.marketplace.service;

import com.training.marketplace.entity.AnonymousWishlistItem;
import com.training.marketplace.entity.Product;
import com.training.marketplace.repository.AnonymousWishlistItemRepository;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.service.impl.AnonymousWishlistServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnonymousWishlistServiceTest {

    @Mock private AnonymousWishlistItemRepository anonymousWishlistRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks private AnonymousWishlistServiceImpl anonymousWishlistService;

    @Test
    void add_existingSessionProductIsIdempotent() {
        AnonymousWishlistItem existing = AnonymousWishlistItem.builder()
                .sessionId("session-1")
                .productId(10L)
                .build();
        when(productRepository.findById(10L)).thenReturn(Optional.of(activeProduct()));
        when(anonymousWishlistRepository.findBySessionIdAndProductId("session-1", 10L))
                .thenReturn(Optional.of(existing));

        var response = anonymousWishlistService.add(" session-1 ", 10L);

        assertThat(response.wishlisted()).isTrue();
        verify(anonymousWishlistRepository, never()).save(any());
    }

    @Test
    void status_readsByNormalizedSession() {
        when(anonymousWishlistRepository.existsBySessionIdAndProductId("session-1", 10L))
                .thenReturn(true);

        var response = anonymousWishlistService.status(" session-1 ", 10L);

        assertThat(response.wishlisted()).isTrue();
    }

    private Product activeProduct() {
        Product product = new Product();
        product.setId(10L);
        product.setActive(true);
        return product;
    }
}