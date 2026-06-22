package com.library.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.management.dto.request.CreateCategoryRequest;
import com.library.management.dto.request.UpdateCategoryRequest;
import com.library.management.dto.response.CategoryResponse;
import com.library.management.exception.handler.GlobalExceptionHandler;
import com.library.management.security.JwtAuthenticationFilter;
import com.library.management.security.SecurityConfig;
import com.library.management.service.CategoryService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CategoryController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@TestPropertySource(properties = "server.servlet.context-path=/")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private com.library.management.repository.UserRepository userRepository;

    @BeforeEach
    void setupFilter() throws ServletException, IOException {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    void createCategory_shouldReturnCreated() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Science");
        request.setDescription("Science desc");

        CategoryResponse response = CategoryResponse.builder()
                .categoryId(1L)
                .name("Science")
                .description("Science desc")
                .bookCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        when(categoryService.createCategory(any(CreateCategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/categories")
                        .with(SecurityMockMvcRequestPostProcessors.user("lib").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Science"));
    }

    @Test
    void getAllCategories_shouldReturnList() throws Exception {
        CategoryResponse response = CategoryResponse.builder()
                .categoryId(1L)
                .name("Science")
                .bookCount(0)
                .build();
        when(categoryService.getAllCategories()).thenReturn(List.of(response));

        mockMvc.perform(get("/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Science"));
    }

    @Test
    void getCategoryById_shouldReturnCategory() throws Exception {
        CategoryResponse response = CategoryResponse.builder()
                .categoryId(2L)
                .name("History")
                .bookCount(3)
                .build();
        when(categoryService.getCategoryById(2L)).thenReturn(response);

        mockMvc.perform(get("/v1/categories/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("History"))
                .andExpect(jsonPath("$.bookCount").value(3));
    }

    @Test
    void updateCategory_shouldReturnUpdated() throws Exception {
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setDescription("Updated");
        CategoryResponse response = CategoryResponse.builder()
                .categoryId(1L)
                .name("Science")
                .description("Updated")
                .bookCount(0)
                .build();

        when(categoryService.updateCategory(any(Long.class), any(UpdateCategoryRequest.class))).thenReturn(response);

        mockMvc.perform(put("/v1/categories/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("lib").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated"));
    }

    @Test
    void searchCategories_shouldReturnList() throws Exception {
        CategoryResponse response = CategoryResponse.builder()
                .categoryId(4L)
                .name("Searchable")
                .bookCount(1)
                .build();
        when(categoryService.searchCategories("Sea")).thenReturn(List.of(response));

        mockMvc.perform(get("/v1/categories/search").param("name", "Sea"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Searchable"));
    }

    @Test
    void deleteCategory_shouldReturnOk() throws Exception {
        mockMvc.perform(delete("/v1/categories/5").with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        verify(categoryService).deleteCategory(5L);
    }
}

