package com.library.management.service;

import com.library.management.dto.request.CreateCategoryRequest;
import com.library.management.dto.request.UpdateCategoryRequest;
import com.library.management.dto.response.CategoryResponse;
import com.library.management.entity.Category;
import com.library.management.exception.DuplicateResourceException;
import com.library.management.exception.InvalidOperationException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.mapper.CategoryMapper;
import com.library.management.repository.CategoryRepository;
import com.library.management.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setup() {
        categoryMapper = new CategoryMapper();
        categoryService = new CategoryServiceImpl(categoryRepository, categoryMapper);
    }

    @Test
    void createCategory_duplicate_throws() {
        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setName("Sci");
        when(categoryRepository.existsByName("Sci")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> categoryService.createCategory(req));
    }

    @Test
    void deleteCategory_withBooks_throws() {
        Category category = Category.builder().categoryId(1L).name("Sci").build();
        category.getBooks().add(new com.library.management.entity.Book());
        when(categoryRepository.findByIdWithBooks(1L)).thenReturn(Optional.of(category));
        assertThrows(InvalidOperationException.class, () -> categoryService.deleteCategory(1L));
    }

    @Test
    void updateCategory_notFound_throws() {
        UpdateCategoryRequest req = new UpdateCategoryRequest();
        req.setName("New");
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> categoryService.updateCategory(1L, req));
    }

    @Test
    void getAllCategories_returnsResponses() {
        Category category = Category.builder().categoryId(1L).name("Sci").build();
        when(categoryRepository.findAllOrderByName()).thenReturn(List.of(category));
        List<CategoryResponse> responses = categoryService.getAllCategories();
        assertEquals(1, responses.size());
        assertEquals("Sci", responses.get(0).getName());
    }
}


