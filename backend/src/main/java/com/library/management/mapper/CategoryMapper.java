package com.library.management.mapper;

import com.library.management.dto.request.CreateCategoryRequest;
import com.library.management.dto.request.UpdateCategoryRequest;
import com.library.management.dto.response.CategoryResponse;
import com.library.management.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponse toCategoryResponse(Category category) {
        if (category == null) {
            return null;
        }
        return CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .description(category.getDescription())
                .bookCount(category.getBookCount())
                .createdAt(category.getCreatedAt())
                .build();
    }

    public Category toEntity(CreateCategoryRequest request) {
        return Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    public void updateEntity(Category category, UpdateCategoryRequest request) {
        if (request.getName() != null) {
            category.setName(request.getName());
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
    }
}


