package com.library.management.service;

import com.library.management.dto.request.BookSearchCriteria;
import com.library.management.dto.request.CreateBookRequest;
import com.library.management.dto.request.UpdateBookRequest;
import com.library.management.entity.Book;
import com.library.management.entity.Category;
import com.library.management.entity.Loan;
import com.library.management.entity.Reservation;
import com.library.management.entity.User;
import com.library.management.enums.BookStatus;
import com.library.management.enums.LoanStatus;
import com.library.management.enums.ReservationStatus;
import com.library.management.enums.UserRole;
import com.library.management.exception.BookNotAvailableException;
import com.library.management.exception.ForbiddenException;
import com.library.management.exception.InvalidOperationException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.mapper.BookMapper;
import com.library.management.repository.BookRepository;
import com.library.management.repository.CategoryRepository;
import com.library.management.repository.UserRepository;
import com.library.management.service.impl.BookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceImplCoverageTest {

    @Mock private BookRepository bookRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private BookMapper bookMapper;
    @Mock private UserRepository userRepository;

    private BookServiceImpl bookService;

    @BeforeEach
    void setup() {
        bookService = new BookServiceImpl(bookRepository, categoryRepository, bookMapper, userRepository);
    }

    // ---------- createBook ----------

    @Test
    void createBook_success_savesWithCategories() {
        CreateBookRequest req = createReq(Set.of(1L, 2L));
        when(bookRepository.existsByIsbn("isbn")).thenReturn(false);
        when(categoryRepository.findAllById(req.getCategoryIds())).thenReturn(List.of(category(1L), category(2L)));
        when(userRepository.findById(9L)).thenReturn(Optional.of(user(9L, UserRole.ADMIN)));
        when(bookMapper.toEntity(req)).thenReturn(book(1L, 5, 5));

        bookService.createBook(req, 9L);

        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void createBook_categoryMissing_throws() {
        CreateBookRequest req = createReq(Set.of(1L, 2L));
        when(bookRepository.existsByIsbn("isbn")).thenReturn(false);
        when(categoryRepository.findAllById(req.getCategoryIds())).thenReturn(List.of(category(1L)));
        assertThrows(ResourceNotFoundException.class, () -> bookService.createBook(req, 9L));
    }

    @Test
    void createBook_creatorMissing_throws() {
        CreateBookRequest req = createReq(Set.of(1L));
        when(bookRepository.existsByIsbn("isbn")).thenReturn(false);
        when(categoryRepository.findAllById(req.getCategoryIds())).thenReturn(List.of(category(1L)));
        when(userRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> bookService.createBook(req, 9L));
    }

    // ---------- getBookById ----------

    @Test
    void getBookById_found() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book(1L, 5, 5)));
        assertDoesNotThrow(() -> bookService.getBookById(1L));
    }

    @Test
    void getBookById_notFound_throws() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> bookService.getBookById(1L));
    }

    // ---------- updateBook ----------

    @Test
    void updateBook_success_adjustsAvailability() {
        Book book = book(1L, 5, 3); // borrowed = 2
        UpdateBookRequest req = new UpdateBookRequest();
        req.setQuantity(4);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(userRepository.findById(9L)).thenReturn(Optional.of(user(9L, UserRole.ADMIN)));

        bookService.updateBook(1L, req, 9L);

        assertEquals(2, book.getAvailableQuantity()); // 4 - 2 borrowed
        verify(bookRepository).save(book);
    }

    @Test
    void updateBook_withNewCategories_replacesThem() {
        Book book = book(1L, 5, 5);
        UpdateBookRequest req = new UpdateBookRequest();
        req.setCategoryIds(Set.of(1L));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(userRepository.findById(9L)).thenReturn(Optional.of(user(9L, UserRole.LIBRARIAN)));
        when(categoryRepository.findAllById(req.getCategoryIds())).thenReturn(List.of(category(1L)));

        bookService.updateBook(1L, req, 9L);

        verify(bookRepository).save(book);
    }

    @Test
    void updateBook_categoryMissing_throws() {
        Book book = book(1L, 5, 5);
        UpdateBookRequest req = new UpdateBookRequest();
        req.setCategoryIds(Set.of(1L, 2L));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(userRepository.findById(9L)).thenReturn(Optional.of(user(9L, UserRole.ADMIN)));
        when(categoryRepository.findAllById(req.getCategoryIds())).thenReturn(List.of(category(1L)));
        assertThrows(ResourceNotFoundException.class, () -> bookService.updateBook(1L, req, 9L));
    }

    @Test
    void updateBook_bookNotFound_throws() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> bookService.updateBook(1L, new UpdateBookRequest(), 9L));
    }

    @Test
    void updateBook_updaterMissing_throws() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book(1L, 5, 5)));
        when(userRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> bookService.updateBook(1L, new UpdateBookRequest(), 9L));
    }

    // ---------- deleteBook ----------

    @Test
    void deleteBook_success_deletes() {
        Book book = book(1L, 5, 5);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(userRepository.findById(9L)).thenReturn(Optional.of(user(9L, UserRole.ADMIN)));

        bookService.deleteBook(1L, 9L);

        verify(bookRepository).delete(book);
    }

    @Test
    void deleteBook_withActiveLoans_throws() {
        Book book = book(1L, 5, 5);
        book.setLoans(List.of(Loan.builder().loanId(1L).status(LoanStatus.ACTIVE).build()));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(userRepository.findById(9L)).thenReturn(Optional.of(user(9L, UserRole.ADMIN)));
        assertThrows(InvalidOperationException.class, () -> bookService.deleteBook(1L, 9L));
    }

    @Test
    void deleteBook_withPendingReservations_throws() {
        Book book = book(1L, 5, 5);
        book.setReservations(List.of(Reservation.builder().reservationId(1L).status(ReservationStatus.PENDING).build()));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(userRepository.findById(9L)).thenReturn(Optional.of(user(9L, UserRole.ADMIN)));
        assertThrows(InvalidOperationException.class, () -> bookService.deleteBook(1L, 9L));
    }

    @Test
    void deleteBook_bookNotFound_throws() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> bookService.deleteBook(1L, 9L));
    }

    @Test
    void deleteBook_deleterMissing_throws() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book(1L, 5, 5)));
        when(userRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> bookService.deleteBook(1L, 9L));
    }

    // ---------- queries ----------

    @Test
    void getAllBooks_returnsList() {
        when(bookRepository.findAll()).thenReturn(List.of(book(1L, 5, 5)));
        assertEquals(1, bookService.getAllBooks().size());
    }

    @Test
    void getBooksByCategory_success() {
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(bookRepository.findByCategoryId(1L)).thenReturn(List.of(book(1L, 5, 5)));
        assertEquals(1, bookService.getBooksByCategory(1L).size());
    }

    @Test
    void getBooksByCategory_categoryMissing_throws() {
        when(categoryRepository.existsById(1L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> bookService.getBooksByCategory(1L));
    }

    @Test
    void getAvailableBooks_returnsList() {
        when(bookRepository.findAllAvailableBooks()).thenReturn(List.of(book(1L, 5, 5)));
        assertEquals(1, bookService.getAvailableBooks().size());
    }

    @Test
    void searchBooks_validSort_returnsPage() {
        BookSearchCriteria criteria = new BookSearchCriteria();
        criteria.setSortBy("author");
        criteria.setSortDirection("DESC");
        Page<Book> page = new PageImpl<>(List.of());
        when(bookRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
        assertNotNull(bookService.searchBooks(criteria));
    }

    @Test
    void searchBooks_invalidSortFallsBackToTitle() {
        BookSearchCriteria criteria = new BookSearchCriteria();
        criteria.setSortBy("totally-unknown");
        Page<Book> page = new PageImpl<>(List.of());
        when(bookRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
        assertNotNull(bookService.searchBooks(criteria));
    }

    // ---------- availability ----------

    @Test
    void decreaseBookAvailability_success() {
        Book book = book(1L, 5, 2);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        bookService.decreaseBookAvailability(1L);
        assertEquals(1, book.getAvailableQuantity());
        verify(bookRepository).save(book);
    }

    @Test
    void decreaseBookAvailability_notAvailable_throws() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book(1L, 5, 0)));
        assertThrows(BookNotAvailableException.class, () -> bookService.decreaseBookAvailability(1L));
    }

    @Test
    void decreaseBookAvailability_notFound_throws() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> bookService.decreaseBookAvailability(1L));
    }

    @Test
    void increaseBookAvailability_success() {
        Book book = book(1L, 5, 1);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        bookService.increaseBookAvailability(1L);
        assertEquals(2, book.getAvailableQuantity());
        verify(bookRepository).save(book);
    }

    @Test
    void increaseBookAvailability_notFound_throws() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> bookService.increaseBookAvailability(1L));
    }

    @Test
    void isBookAvailable_trueForAvailableBook() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book(1L, 5, 3)));
        assertTrue(bookService.isBookAvailable(1L));
    }

    @Test
    void isBookAvailable_falseWhenMissing() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());
        assertFalse(bookService.isBookAvailable(1L));
    }

    // ---------- helpers ----------

    private CreateBookRequest createReq(Set<Long> categoryIds) {
        CreateBookRequest r = new CreateBookRequest();
        r.setTitle("t");
        r.setAuthor("a");
        r.setIsbn("isbn");
        r.setPublicationYear(2008);
        r.setQuantity(5);
        r.setCategoryIds(categoryIds);
        return r;
    }

    private Book book(Long id, int quantity, int available) {
        return Book.builder().bookId(id).title("t").author("a").isbn("i" + id)
                .quantity(quantity).availableQuantity(available).status(BookStatus.AVAILABLE).build();
    }

    private Category category(Long id) {
        return Category.builder().categoryId(id).name("c" + id).build();
    }

    private User user(Long id, UserRole role) {
        return User.builder().userId(id).email("u" + id + "@test.com").firstName("F").lastName("L")
                .role(role).isActive(true).build();
    }
}
