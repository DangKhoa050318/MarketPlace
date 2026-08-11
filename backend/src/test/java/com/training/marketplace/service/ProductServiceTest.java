package com.training.marketplace.service;

import com.training.marketplace.dto.request.CreateProductRequest;
import com.training.marketplace.dto.request.UpdateProductRequest;
import com.training.marketplace.dto.response.ProductResponse;
import com.training.marketplace.entity.Product;
import com.training.marketplace.exception.DuplicateResourceException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.mapper.ProductMapper;
import com.training.marketplace.mapper.ProductVariantMapper;
import com.training.marketplace.repository.CategoryRepository;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.ProductVariantRepository;
import com.training.marketplace.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductMapper productMapper;
    @Mock private ProductVariantMapper variantMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private ProductServiceImpl productService;

    @Test
    void create_validRequest_returnsProductResponse() {
        var request = new CreateProductRequest("phone", "Phone", "Desc", 1L, "PCS", "http://img.jpg");
        var product = buildProduct(1L, "phone", "Phone");
        var response = buildResponse(1L, "phone", "Phone");

        when(productRepository.existsBySlug("phone")).thenReturn(false);
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(productMapper.toEntity(request)).thenReturn(product);
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        var result = productService.create(request);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Phone");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void create_duplicateSlug_throwsDuplicateResourceException() {
        var request = new CreateProductRequest("phone", "Phone", "Desc", 1L, "PCS", "http://img.jpg");
        when(productRepository.existsBySlug("phone")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void create_categoryNotFound_throwsResourceNotFoundException() {
        var request = new CreateProductRequest("phone", "Phone", "Desc", 99L, "PCS", "http://img.jpg");
        when(productRepository.existsBySlug("phone")).thenReturn(false);
        when(categoryRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void getById_found_returnsProductWithVariants() {
        var product = buildProduct(1L, "phone", "Phone");
        var response = buildResponse(1L, "phone", "Phone");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);
        when(variantRepository.findByProductId(1L)).thenReturn(List.of());
        when(variantMapper.toResponseList(List.of())).thenReturn(List.of());

        var result = productService.getById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Phone");
        assertThat(result.variants()).isEmpty();
    }

    @Test
    void getById_notFound_throwsResourceNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void search_validQuery_returnsMatchingProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        var product = buildProduct(1L, "phone", "Phone");
        var response = buildResponse(1L, "phone", "Phone");

        when(productRepository.searchProducts("Phone", pageable)).thenReturn(new PageImpl<>(List.of(product)));
        when(productMapper.toResponse(product)).thenReturn(response);

        var result = productService.search("Phone", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Phone");
    }

    @Test
    void update_validRequest_returnsUpdatedResponse() {
        var updateRequest = new UpdateProductRequest("Smart Phone", "New Desc", 1L, "PCS", "http://newimg.jpg", true);
        var product = buildProduct(1L, "phone", "Phone");
        var response = buildResponse(1L, "phone", "Smart Phone");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        var result = productService.update(1L, updateRequest);

        assertThat(result.name()).isEqualTo("Smart Phone");
    }

    @Test
    void delete_existingProduct_setsActiveFalse() {
        var product = buildProduct(1L, "phone", "Phone");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.delete(1L);

        assertThat(product.isActive()).isFalse();
        verify(productRepository).save(product);
    }

    private Product buildProduct(Long id, String slug, String name) {
        Product product = Product.builder()
                .slug(slug).name(name).description("Desc").categoryId(1L).unit("PCS")
                .imageUrl("http://img.jpg").active(true).build();
        product.setId(id);
        return product;
    }

    private ProductResponse buildResponse(Long id, String slug, String name) {
        return new ProductResponse(id, slug, name, "Desc", 1L, "PCS", "http://img.jpg", true,
                LocalDateTime.now(), LocalDateTime.now(), null);
    }
}
