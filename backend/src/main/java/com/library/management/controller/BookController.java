package com.library.management.controller;

import com.library.management.dto.request.BookSearchCriteria;
import com.library.management.dto.request.CreateBookRequest;
import com.library.management.dto.request.UpdateBookRequest;
import com.library.management.dto.response.BookResponse;
import com.library.management.dto.response.BookSummaryResponse;
import com.library.management.dto.response.MessageResponse;
import com.library.management.dto.response.PageResponse;
import com.library.management.entity.User;
import com.library.management.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/books")
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
@Validated
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @PostMapping
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody CreateBookRequest request,
                                                   @AuthenticationPrincipal User user) {
        Long userId = user.getUserId();
        log.info("Creating book: {} by user {}", request.getTitle(), userId);
        BookResponse response = bookService.createBook(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<BookResponse>> getAllBooks() {
        return ResponseEntity.ok(bookService.getAllBooks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBookById(@PathVariable Long id) {
        log.info("Fetching book with ID: {}", id);
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<BookResponse> updateBook(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateBookRequest request,
                                                   @AuthenticationPrincipal User user) {
        Long userId = user.getUserId();
        log.info("Updating book ID: {} by user {}", id, userId);
        return ResponseEntity.ok(bookService.updateBook(id, request, userId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteBook(@PathVariable Long id,
                                                      @AuthenticationPrincipal User user) {
        Long userId = user.getUserId();
        log.info("Deleting book ID: {} by user {}", id, userId);
        bookService.deleteBook(id, userId);
        return ResponseEntity.ok(MessageResponse.builder().message("Book deleted successfully").build());
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<BookSummaryResponse>> searchBooks(@Valid @ModelAttribute BookSearchCriteria criteria) {
        log.info("Searching books with criteria: {}", criteria);
        return ResponseEntity.ok(bookService.searchBooks(criteria));
    }

    @GetMapping("/available")
    public ResponseEntity<List<BookResponse>> getAvailableBooks() {
        return ResponseEntity.ok(bookService.getAvailableBooks());
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<BookResponse>> getBooksByCategory(@PathVariable Long categoryId) {
        log.info("Fetching books for category ID: {}", categoryId);
        return ResponseEntity.ok(bookService.getBooksByCategory(categoryId));
    }
}

