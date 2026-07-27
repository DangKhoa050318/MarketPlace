package com.training.marketplace.service.impl;

import com.training.marketplace.dto.request.CreateCategoryRequest;
import com.training.marketplace.dto.request.UpdateCategoryRequest;
import com.training.marketplace.dto.response.CategoryResponse;
import com.training.marketplace.entity.Category;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.DuplicateResourceException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.mapper.CategoryMapper;
import com.training.marketplace.repository.CategoryRepository;
import com.training.marketplace.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        if (categoryRepository.existsBySlug(request.slug())) {
            throw new DuplicateResourceException("Category", "slug", request.slug());
        }
        if (categoryRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Category", "code", request.code());
        }
        if (request.parentId() != null && !categoryRepository.existsById(request.parentId())) {
            throw new ResourceNotFoundException("Category", request.parentId());
        }
        Category category = categoryMapper.toEntity(request);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        if (request.slug() != null && !request.slug().equals(category.getSlug())
                && categoryRepository.existsBySlug(request.slug())) {
            throw new DuplicateResourceException("Category", "slug", request.slug());
        }
        if (request.parentId() != null) {
            validateParent(id, request.parentId());
        }

        categoryMapper.updateEntity(category, request);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        categoryRepository.delete(category);
    }

    /**
     * A category cannot be its own parent, the parent must exist, and setting it must not create a
     * cycle — walk the ancestor chain from the proposed parent and reject if we reach {@code id}.
     */
    private void validateParent(Long id, Long parentId) {
        if (parentId.equals(id)) {
            throw new BadRequestException("A category cannot be its own parent");
        }
        Long cursor = parentId;
        while (cursor != null) {
            if (cursor.equals(id)) {
                throw new BadRequestException("Category parent assignment would create a cycle");
            }
            Category ancestor = categoryRepository.findById(cursor)
                    .orElseThrow(() -> new ResourceNotFoundException("Category", parentId));
            cursor = ancestor.getParentId();
        }
    }
}
