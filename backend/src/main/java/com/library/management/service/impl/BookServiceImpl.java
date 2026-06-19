package com.library.management.service.impl;

import com.library.management.dto.request.BookSearchCriteria;
import com.library.management.dto.request.CreateBookRequest;
import com.library.management.dto.request.UpdateBookRequest;
import com.library.management.dto.response.BookResponse;
import com.library.management.dto.response.BookSummaryResponse;
import com.library.management.dto.response.PageResponse;
import com.library.management.entity.Book;
import com.library.management.entity.Category;
import com.library.management.entity.User;
import com.library.management.enums.BookStatus;
import com.library.management.enums.UserRole;
import com.library.management.exception.BookNotAvailableException;
import com.library.management.exception.DuplicateResourceException;
import com.library.management.exception.ForbiddenException;
import com.library.management.exception.InvalidOperationException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.mapper.BookMapper;
import com.library.management.repository.BookRepository;
import com.library.management.repository.BookSpecification;
import com.library.management.repository.CategoryRepository;
import com.library.management.repository.UserRepository;
import com.library.management.service.BookService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final BookMapper bookMapper;
    private final UserRepository userRepository;

    @Override
    public BookResponse createBook(CreateBookRequest request, Long createdBy) {
        if (bookRepository.existsByIsbn(request.getIsbn())) {
            throw new DuplicateResourceException("Book", "isbn", request.getIsbn());
        }

        List<Category> categories = categoryRepository.findAllById(request.getCategoryIds());
        if (categories.size() != request.getCategoryIds().size()) {
            throw new ResourceNotFoundException("Category", "ids", request.getCategoryIds());
        }

        User creator = userRepository.findById(createdBy)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", createdBy));
        if (!(creator.isAdmin() || creator.isLibrarian())) {
            throw new ForbiddenException("Only librarians and admins can add books");
        }

        Book book = bookMapper.toEntity(request);
        categories.forEach(book::addCategory);

        bookRepository.save(book);
        log.info("Book created: {} (ISBN: {}) by user {}", book.getTitle(), book.getIsbn(), createdBy);
        return bookMapper.toBookResponse(book);
    }

    @Override
    public BookResponse getBookById(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", bookId));
        return bookMapper.toBookResponse(book);
    }

    @Override
    public BookResponse updateBook(Long bookId, UpdateBookRequest request, Long updatedBy) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", bookId));

        User updater = userRepository.findById(updatedBy)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", updatedBy));
        if (!(updater.isAdmin() || updater.isLibrarian())) {
            throw new ForbiddenException("Only librarians and admins can update books");
        }

        if (request.getQuantity() != null) {
            int borrowed = book.getQuantity() - book.getAvailableQuantity();
            if (request.getQuantity() < borrowed) {
                throw new InvalidOperationException("Cannot reduce quantity below currently borrowed amount");
            }
            int newAvailable = request.getQuantity() - borrowed;
            book.setAvailableQuantity(newAvailable);
        }

        bookMapper.updateEntity(book, request);

        if (request.getCategoryIds() != null) {
            List<Category> newCategories = categoryRepository.findAllById(request.getCategoryIds());
            if (newCategories.size() != request.getCategoryIds().size()) {
                throw new ResourceNotFoundException("Category", "ids", request.getCategoryIds());
            }
            book.getCategories().clear();
            newCategories.forEach(book::addCategory);
        }

        bookRepository.save(book);
        log.info("Book updated: {} (ID: {}) by user {}", book.getTitle(), bookId, updatedBy);
        return bookMapper.toBookResponse(book);
    }

    @Override
    public void deleteBook(Long bookId, Long deletedBy) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", bookId));

        User deleter = userRepository.findById(deletedBy)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", deletedBy));
        if (!deleter.isAdmin()) {
            throw new ForbiddenException("Only admins can delete books");
        }

        if (book.hasActiveLoans()) {
            throw new InvalidOperationException("Cannot delete book with active loans. Wait for all loans to be returned.");
        }
        if (book.getPendingReservationsCount() > 0) {
            throw new InvalidOperationException("Cannot delete book with pending reservations");
        }

        bookRepository.delete(book);
        log.info("Book deleted: {} (ID: {}) by admin {}", book.getTitle(), bookId, deletedBy);
    }

    @Override
    public List<BookResponse> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(bookMapper::toBookResponse)
                .toList();
    }

    @Override
    public PageResponse<BookSummaryResponse> searchBooks(BookSearchCriteria criteria) {
        Sort sort = buildSort(criteria.getSortBy(), criteria.sortDir());
        PageRequest pageRequest = PageRequest.of(criteria.getPage(), criteria.getSize(), sort);
        Page<Book> page = bookRepository.findAll(BookSpecification.withFilters(criteria), pageRequest);
        Page<BookSummaryResponse> mapped = page.map(bookMapper::toBookSummaryResponse);
        log.info("Book search executed with criteria: {}", criteria);
        return PageResponse.of(mapped);
    }

    private Sort buildSort(String sortBy, Sort.Direction direction) {
        String key = sortBy;
        if (!StringUtils.hasText(sortBy) ||
                !(sortBy.equalsIgnoreCase("title") || sortBy.equalsIgnoreCase("author")
                        || sortBy.equalsIgnoreCase("publicationYear") || sortBy.equalsIgnoreCase("createdAt"))) {
            key = "title";
        }
        return Sort.by(direction, key);
    }

    @Override
    public List<BookResponse> getBooksByCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category", "id", categoryId);
        }
        return bookRepository.findByCategoryId(categoryId).stream()
                .map(bookMapper::toBookResponse)
                .toList();
    }

    @Override
    public List<BookResponse> getAvailableBooks() {
        return bookRepository.findAllAvailableBooks().stream()
                .map(bookMapper::toBookResponse)
                .toList();
    }

    @Override
    public void decreaseBookAvailability(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", bookId));
        if (!book.isAvailable()) {
            throw new BookNotAvailableException("No copies available for book: " + book.getTitle());
        }
        book.decreaseAvailableQuantity();
        bookRepository.save(book);
        log.info("Book availability decreased: {} (ID: {}), available: {}", book.getTitle(), bookId, book.getAvailableQuantity());
    }

    @Override
    public void increaseBookAvailability(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", bookId));
        book.increaseAvailableQuantity();
        bookRepository.save(book);
        log.info("Book availability increased: {} (ID: {}), available: {}", book.getTitle(), bookId, book.getAvailableQuantity());
    }

    @Override
    public boolean isBookAvailable(Long bookId) {
        return bookRepository.findById(bookId).map(Book::isAvailable).orElse(false);
    }
}


