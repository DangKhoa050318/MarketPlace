package com.training.marketplace.service;

import com.training.marketplace.dto.request.CreateCategoryRequest;
import com.training.marketplace.dto.request.UpdateCategoryRequest;
import com.training.marketplace.dto.response.CategoryResponse;
import com.training.marketplace.entity.Category;
import com.training.marketplace.exception.DuplicateResourceException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.mapper.CategoryMapper;
import com.training.marketplace.repository.CategoryRepository;
import com.training.marketplace.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private CategoryMapper categoryMapper;

    @InjectMocks private CategoryServiceImpl categoryService;

    @Test
    void create_validRequest_returnsCategoryResponse() {
        var request = new CreateCategoryRequest("Electronics", "ELEC", "electronics", null);
        var entity = buildCategory(1L, "Electronics", "ELEC", "electronics");
        var response = buildResponse(1L, "Electronics", "ELEC", "electronics");

        when(categoryRepository.existsBySlug("electronics")).thenReturn(false);
        when(categoryRepository.existsByCode("ELEC")).thenReturn(false);
        when(categoryMapper.toEntity(request)).thenReturn(entity);
        when(categoryRepository.save(any(Category.class))).thenReturn(entity);
        when(categoryMapper.toResponse(entity)).thenReturn(response);

        var result = categoryService.create(request);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Electronics");
        assertThat(result.code()).isEqualTo("ELEC");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void create_duplicateSlug_throwsDuplicateResourceException() {
        var request = new CreateCategoryRequest("Electronics", "ELEC", "electronics", null);
        when(categoryRepository.existsBySlug("electronics")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void getById_found_returnsCategoryResponse() {
        var entity = buildCategory(1L, "Books", "BOOK", "books");
        var response = buildResponse(1L, "Books", "BOOK", "books");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(categoryMapper.toResponse(entity)).thenReturn(response);

        var result = categoryService.getById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Books");
    }

    @Test
    void getById_notFound_throwsResourceNotFoundException() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> categoryService.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAll_returnsCategoryList() {
        var c1 = buildCategory(1L, "Electronics", "ELEC", "electronics");
        var c2 = buildCategory(2L, "Books", "BOOK", "books");
        when(categoryRepository.findAll()).thenReturn(List.of(c1, c2));
        when(categoryMapper.toResponse(c1)).thenReturn(buildResponse(1L, "Electronics", "ELEC", "electronics"));
        when(categoryMapper.toResponse(c2)).thenReturn(buildResponse(2L, "Books", "BOOK", "books"));

        var results = categoryService.getAll();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).name()).isEqualTo("Electronics");
    }

    @Test
    void update_validRequest_updatesAndReturns() {
        var entity = buildCategory(1L, "Old Name", "OLD", "old-slug");
        var request = new UpdateCategoryRequest("New Name", "new-slug", null);
        var updatedResponse = buildResponse(1L, "New Name", "OLD", "new-slug");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(categoryRepository.existsBySlug("new-slug")).thenReturn(false);
        when(categoryRepository.save(entity)).thenReturn(entity);
        when(categoryMapper.toResponse(entity)).thenReturn(updatedResponse);

        var result = categoryService.update(1L, request);

        assertThat(result.name()).isEqualTo("New Name");
        verify(categoryMapper).updateEntity(entity, request);
        verify(categoryRepository).save(entity);
    }

    @Test
    void update_notFound_throwsResourceNotFoundException() {
        var request = new UpdateCategoryRequest("New Name", "new-slug", null);
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> categoryService.update(999L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_duplicateSlug_throwsDuplicateResourceException() {
        var entity = buildCategory(1L, "Category One", "C1", "cat-1");
        var request = new UpdateCategoryRequest("Category One Updated", "cat-2", null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(categoryRepository.existsBySlug("cat-2")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.update(1L, request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void delete_existingCategory_deletesSuccessfully() {
        var entity = buildCategory(1L, "Category To Delete", "DEL", "to-delete");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(entity));
        categoryService.delete(1L);
        verify(categoryRepository).delete(entity);
    }

    @Test
    void delete_notFound_throwsResourceNotFoundException() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> categoryService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(categoryRepository, never()).delete(any());
    }

    private Category buildCategory(Long id, String name, String code, String slug) {
        Category category = Category.builder().name(name).code(code).slug(slug).build();
        category.setId(id);
        return category;
    }

    private CategoryResponse buildResponse(Long id, String name, String code, String slug) {
        return new CategoryResponse(id, name, code, slug, null, LocalDateTime.now(), LocalDateTime.now());
    }
}
