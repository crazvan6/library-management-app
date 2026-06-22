package com.library.management.service;

import com.library.management.dto.request.BookSearchCriteria;
import com.library.management.dto.request.CreateBookRequest;
import com.library.management.dto.request.UpdateBookRequest;
import com.library.management.dto.response.BookResponse;
import com.library.management.dto.response.PageResponse;
import com.library.management.entity.Book;
import com.library.management.entity.Category;
import com.library.management.entity.User;
import com.library.management.enums.BookStatus;
import com.library.management.enums.UserRole;
import com.library.management.exception.DuplicateResourceException;
import com.library.management.exception.ForbiddenException;
import com.library.management.exception.InvalidOperationException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.mapper.BookMapper;
import com.library.management.repository.BookRepository;
import com.library.management.repository.BookSpecification;
import com.library.management.repository.CategoryRepository;
import com.library.management.repository.UserRepository;
import com.library.management.service.impl.BookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;

    private BookMapper bookMapper;

    @InjectMocks
    private BookServiceImpl bookService;

    @BeforeEach
    void setup() {
        bookMapper = new BookMapper();
        bookService = new BookServiceImpl(bookRepository, categoryRepository, bookMapper, userRepository);
    }

    @Test
    void createBook_duplicateIsbn_throws() {
        CreateBookRequest request = new CreateBookRequest();
        request.setIsbn("123");
        request.setTitle("t");
        request.setAuthor("a");
        request.setQuantity(1);
        request.setCategoryIds(Set.of(1L));

        when(bookRepository.existsByIsbn("123")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> bookService.createBook(request, 1L));
    }

    @Test
    void createBook_nonLibrarianForbidden() {
        CreateBookRequest request = minimalCreateReq();
        when(bookRepository.existsByIsbn(any())).thenReturn(false);
        when(categoryRepository.findAllById(any())).thenReturn(List.of(new Category()));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(UserRole.STUDENT)));

        assertThrows(ForbiddenException.class, () -> bookService.createBook(request, 1L));
    }

    @Test
    void updateBook_quantityBelowBorrowed_throws() {
        Book book = Book.builder().bookId(1L).quantity(5).availableQuantity(2).build(); // borrowed =3
        UpdateBookRequest req = new UpdateBookRequest();
        req.setQuantity(2);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(UserRole.ADMIN)));

        assertThrows(InvalidOperationException.class, () -> bookService.updateBook(1L, req, 2L));
    }

    @Test
    void deleteBook_notAdmin_forbidden() {
        Book book = Book.builder().bookId(1L).build();
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(UserRole.LIBRARIAN)));

        assertThrows(ForbiddenException.class, () -> bookService.deleteBook(1L, 2L));
    }

    @Test
    void searchBooks_returnsPage() {
        BookSearchCriteria criteria = new BookSearchCriteria();
        Page<Book> page = new PageImpl<>(List.of(Book.builder().bookId(1L).availableQuantity(1).status(BookStatus.AVAILABLE).build()));
        when(bookRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class))).thenReturn(page);

        PageResponse<?> response = bookService.searchBooks(criteria);
        assertEquals(1, response.getTotalElements());
    }

    private CreateBookRequest minimalCreateReq() {
        CreateBookRequest request = new CreateBookRequest();
        request.setIsbn("123");
        request.setTitle("t");
        request.setAuthor("a");
        request.setQuantity(1);
        request.setCategoryIds(Set.of(1L));
        return request;
    }

    private User user(UserRole role) {
        return User.builder().userId(1L).role(role).email("u@test").isActive(true).build();
    }
}

