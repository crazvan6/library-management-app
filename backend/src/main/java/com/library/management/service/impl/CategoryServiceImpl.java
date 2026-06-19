package com.library.management.service.impl;

import com.library.management.dto.request.CreateCategoryRequest;
import com.library.management.dto.request.UpdateCategoryRequest;
import com.library.management.dto.response.CategoryResponse;
import com.library.management.entity.Category;
import com.library.management.exception.DuplicateResourceException;
import com.library.management.exception.InvalidOperationException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.mapper.CategoryMapper;
import com.library.management.repository.CategoryRepository;
import com.library.management.service.CategoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category", "name", request.getName());
        }
        Category category = categoryMapper.toEntity(request);
        categoryRepository.save(category);
        log.info("Category created: {}", category.getName());
        return categoryMapper.toCategoryResponse(category);
    }

    @Override
    public CategoryResponse getCategoryById(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
        return categoryMapper.toCategoryResponse(category);
    }

    @Override
    public CategoryResponse updateCategory(Long categoryId, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));

        if (request.getName() != null && categoryRepository.existsByName(request.getName())
                && !category.getName().equalsIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Category", "name", request.getName());
        }

        categoryMapper.updateEntity(category, request);
        categoryRepository.save(category);
        log.info("Category updated: {} (ID: {})", category.getName(), categoryId);
        return categoryMapper.toCategoryResponse(category);
    }

    @Override
    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findByIdWithBooks(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
        if (category.getBookCount() > 0) {
            throw new InvalidOperationException("Cannot delete category with associated books. Remove books first or reassign them.");
        }
        categoryRepository.delete(category);
        log.info("Category deleted: {} (ID: {})", category.getName(), categoryId);
    }

    @Override
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAllOrderByName().stream()
                .map(categoryMapper::toCategoryResponse)
                .toList();
    }

    @Override
    public List<CategoryResponse> searchCategories(String name) {
        return categoryRepository.findByNameContainingIgnoreCase(name).stream()
                .map(categoryMapper::toCategoryResponse)
                .toList();
    }
}


