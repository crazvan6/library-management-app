package com.library.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.management.dto.request.BookSearchCriteria;
import com.library.management.dto.request.CreateBookRequest;
import com.library.management.dto.request.UpdateBookRequest;
import com.library.management.dto.response.BookResponse;
import com.library.management.dto.response.BookSummaryResponse;
import com.library.management.dto.response.PageResponse;
import com.library.management.entity.User;
import com.library.management.enums.BookStatus;
import com.library.management.enums.UserRole;
import com.library.management.exception.handler.GlobalExceptionHandler;
import com.library.management.security.JwtAuthenticationFilter;
import com.library.management.security.SecurityConfig;
import com.library.management.service.BookService;
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
import java.util.Set;

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

@WebMvcTest(controllers = BookController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@TestPropertySource(properties = "server.servlet.context-path=/")
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookService bookService;

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
    void createBook_shouldReturnCreated() throws Exception {
        CreateBookRequest request = new CreateBookRequest();
        request.setTitle("Clean Code");
        request.setAuthor("Robert Martin");
        request.setIsbn("9780132350884");
        request.setPublicationYear(2008);
        request.setQuantity(5);
        request.setCategoryIds(Set.of(1L));

        BookResponse response = BookResponse.builder()
                .bookId(10L)
                .title(request.getTitle())
                .author(request.getAuthor())
                .isbn(request.getIsbn())
                .publicationYear(request.getPublicationYear())
                .quantity(request.getQuantity())
                .availableQuantity(5)
                .status(BookStatus.AVAILABLE)
                .isAvailable(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(bookService.createBook(any(CreateBookRequest.class), any())).thenReturn(response);

        User librarian = User.builder().userId(50L).role(UserRole.LIBRARIAN).email("lib@example.com").isActive(true).build();

        mockMvc.perform(post("/v1/books")
                        .with(SecurityMockMvcRequestPostProcessors.user(librarian))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.isbn").value("9780132350884"));
    }

    @Test
    void getBookById_shouldReturnBook() throws Exception {
        BookResponse response = BookResponse.builder()
                .bookId(10L)
                .title("DDD")
                .author("Eric Evans")
                .isbn("1234567890")
                .availableQuantity(2)
                .status(BookStatus.AVAILABLE)
                .isAvailable(true)
                .build();

        when(bookService.getBookById(10L)).thenReturn(response);

        mockMvc.perform(get("/v1/books/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("DDD"))
                .andExpect(jsonPath("$.isbn").value("1234567890"));
    }

    @Test
    void getAllBooks_shouldReturnList() throws Exception {
        BookResponse response = BookResponse.builder()
                .bookId(1L)
                .title("Book 1")
                .author("Author 1")
                .isbn("111")
                .availableQuantity(2)
                .status(BookStatus.AVAILABLE)
                .isAvailable(true)
                .build();

        when(bookService.getAllBooks()).thenReturn(List.of(response));

        mockMvc.perform(get("/v1/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Book 1"));
    }

    @Test
    void updateBook_shouldReturnUpdated() throws Exception {
        UpdateBookRequest request = new UpdateBookRequest();
        request.setTitle("Updated");
        request.setQuantity(3);

        BookResponse response = BookResponse.builder()
                .bookId(10L)
                .title("Updated")
                .quantity(3)
                .availableQuantity(3)
                .status(BookStatus.AVAILABLE)
                .isAvailable(true)
                .build();

        when(bookService.updateBook(any(Long.class), any(UpdateBookRequest.class), any())).thenReturn(response);

        User librarian = User.builder().userId(2L).role(UserRole.LIBRARIAN).isActive(true).build();

        mockMvc.perform(put("/v1/books/10")
                        .with(SecurityMockMvcRequestPostProcessors.user(librarian))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"))
                .andExpect(jsonPath("$.quantity").value(3));
    }

    @Test
    void deleteBook_shouldRequireAdmin() throws Exception {
        User admin = User.builder().userId(1L).role(UserRole.ADMIN).email("admin@test").isActive(true).build();
        mockMvc.perform(delete("/v1/books/5").with(SecurityMockMvcRequestPostProcessors.user(admin)))
                .andExpect(status().isOk());
        verify(bookService).deleteBook(5L, 1L);
    }

    @Test
    void getAvailableBooks_shouldReturnList() throws Exception {
        BookResponse response = BookResponse.builder()
                .bookId(2L)
                .title("Available")
                .isbn("222")
                .availableQuantity(1)
                .status(BookStatus.AVAILABLE)
                .isAvailable(true)
                .build();
        when(bookService.getAvailableBooks()).thenReturn(List.of(response));

        mockMvc.perform(get("/v1/books/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Available"));
    }

    @Test
    void getBooksByCategory_shouldReturnList() throws Exception {
        BookResponse response = BookResponse.builder()
                .bookId(3L)
                .title("By Category")
                .isbn("333")
                .availableQuantity(2)
                .status(BookStatus.AVAILABLE)
                .isAvailable(true)
                .build();
        when(bookService.getBooksByCategory(7L)).thenReturn(List.of(response));

        mockMvc.perform(get("/v1/books/category/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookId").value(3L));
    }

    @Test
    void searchBooks_shouldReturnPage() throws Exception {
        BookSummaryResponse summary = BookSummaryResponse.builder()
                .bookId(1L)
                .title("Test")
                .author("Author")
                .isbn("111")
                .availableQuantity(1)
                .status(BookStatus.AVAILABLE)
                .isAvailable(true)
                .categoryNames(List.of("Cat"))
                .build();

        PageResponse<BookSummaryResponse> page = PageResponse.<BookSummaryResponse>builder()
                .content(List.of(summary))
                .totalElements(1L)
                .totalPages(1)
                .currentPage(0)
                .pageSize(20)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(bookService.searchBooks(any(BookSearchCriteria.class))).thenReturn(page);

        mockMvc.perform(get("/v1/books/search")
                        .param("title", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Test"));
    }
}

