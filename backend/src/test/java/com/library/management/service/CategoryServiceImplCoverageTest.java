package com.library.management.service;

import com.library.management.dto.request.CreateCategoryRequest;
import com.library.management.dto.request.UpdateCategoryRequest;
import com.library.management.entity.Book;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplCoverageTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private CategoryMapper categoryMapper;

    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setup() {
        categoryService = new CategoryServiceImpl(categoryRepository, categoryMapper);
    }

    @Test
    void createCategory_success_saves() {
        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setName("Software");
        when(categoryRepository.existsByName("Software")).thenReturn(false);
        when(categoryMapper.toEntity(req)).thenReturn(category(1L, "Software"));

        categoryService.createCategory(req);

        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategory_duplicate_throws() {
        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setName("Software");
        when(categoryRepository.existsByName("Software")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> categoryService.createCategory(req));
    }

    @Test
    void getCategoryById_found() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Software")));
        assertDoesNotThrow(() -> categoryService.getCategoryById(1L));
    }

    @Test
    void getCategoryById_notFound_throws() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> categoryService.getCategoryById(1L));
    }

    @Test
    void updateCategory_newUniqueName_succeeds() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Old")));
        UpdateCategoryRequest req = new UpdateCategoryRequest();
        req.setName("New");
        when(categoryRepository.existsByName("New")).thenReturn(false);

        categoryService.updateCategory(1L, req);

        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void updateCategory_sameNameDifferentCase_noDuplicate() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Software")));
        UpdateCategoryRequest req = new UpdateCategoryRequest();
        req.setName("software");
        when(categoryRepository.existsByName("software")).thenReturn(true);

        categoryService.updateCategory(1L, req);

        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void updateCategory_duplicateName_throws() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Old")));
        UpdateCategoryRequest req = new UpdateCategoryRequest();
        req.setName("Existing");
        when(categoryRepository.existsByName("Existing")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> categoryService.updateCategory(1L, req));
    }

    @Test
    void updateCategory_notFound_throws() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> categoryService.updateCategory(1L, new UpdateCategoryRequest()));
    }

    @Test
    void deleteCategory_success_deletes() {
        Category category = category(1L, "Empty");
        when(categoryRepository.findByIdWithBooks(1L)).thenReturn(Optional.of(category));
        categoryService.deleteCategory(1L);
        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteCategory_withBooks_throws() {
        Category category = category(1L, "HasBooks");
        category.getBooks().add(Book.builder().bookId(1L).build());
        when(categoryRepository.findByIdWithBooks(1L)).thenReturn(Optional.of(category));
        assertThrows(InvalidOperationException.class, () -> categoryService.deleteCategory(1L));
    }

    @Test
    void deleteCategory_notFound_throws() {
        when(categoryRepository.findByIdWithBooks(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> categoryService.deleteCategory(1L));
    }

    @Test
    void getAllCategories_returnsList() {
        when(categoryRepository.findAllOrderByName()).thenReturn(List.of(category(1L, "A"), category(2L, "B")));
        assertEquals(2, categoryService.getAllCategories().size());
    }

    @Test
    void searchCategories_returnsList() {
        when(categoryRepository.findByNameContainingIgnoreCase("soft")).thenReturn(List.of(category(1L, "Software")));
        assertEquals(1, categoryService.searchCategories("soft").size());
    }

    private Category category(Long id, String name) {
        return Category.builder().categoryId(id).name(name).build();
    }
}
