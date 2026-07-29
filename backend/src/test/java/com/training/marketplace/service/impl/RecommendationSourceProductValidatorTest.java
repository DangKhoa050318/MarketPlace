package com.training.marketplace.service.impl;

import com.training.marketplace.analytics.RecommendationStrategyType;
import com.training.marketplace.entity.Product;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationSourceProductValidatorTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private RecommendationSourceProductValidator validator;

    @Test
    void requireActive_returnsActiveSourceProduct() {
        Product product = product(10L, true);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThat(validator.requireActive(10L, RecommendationStrategyType.CO_VIEWED))
                .isSameAs(product);
    }

    @Test
    void requireActive_rejectsMissingSourceId() {
        assertThatThrownBy(() -> validator.requireActive(
                null, RecommendationStrategyType.CO_VIEWED))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Source product ID");
    }

    @Test
    void requireActive_rejectsUnknownOrInactiveProduct() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> validator.requireActive(
                99L, RecommendationStrategyType.CO_PURCHASED))
                .isInstanceOf(ResourceNotFoundException.class);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product(10L, false)));
        assertThatThrownBy(() -> validator.requireActive(
                10L, RecommendationStrategyType.CO_PURCHASED))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not active");
    }

    private Product product(Long id, boolean active) {
        Product product = Product.builder()
                .slug("product-" + id)
                .name("Product " + id)
                .active(active)
                .build();
        product.setId(id);
        return product;
    }
}
