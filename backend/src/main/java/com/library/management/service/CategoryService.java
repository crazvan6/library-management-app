package com.library.management.service;

import com.library.management.dto.request.CreateCategoryRequest;
import com.library.management.dto.request.UpdateCategoryRequest;
import com.library.management.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CreateCategoryRequest request);

    CategoryResponse getCategoryById(Long categoryId);

    CategoryResponse updateCategory(Long categoryId, UpdateCategoryRequest request);

    void deleteCategory(Long categoryId);

    List<CategoryResponse> getAllCategories();

    List<CategoryResponse> searchCategories(String name);
}


