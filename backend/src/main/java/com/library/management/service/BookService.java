package com.library.management.service;

import com.library.management.dto.request.BookSearchCriteria;
import com.library.management.dto.request.CreateBookRequest;
import com.library.management.dto.request.UpdateBookRequest;
import com.library.management.dto.response.BookResponse;
import com.library.management.dto.response.BookSummaryResponse;
import com.library.management.dto.response.PageResponse;

import java.util.List;

public interface BookService {
    BookResponse createBook(CreateBookRequest request, Long createdBy);

    BookResponse getBookById(Long bookId);

    BookResponse updateBook(Long bookId, UpdateBookRequest request, Long updatedBy);

    void deleteBook(Long bookId, Long deletedBy);

    List<BookResponse> getAllBooks();

    PageResponse<BookSummaryResponse> searchBooks(BookSearchCriteria criteria);

    List<BookResponse> getBooksByCategory(Long categoryId);

    List<BookResponse> getAvailableBooks();

    void decreaseBookAvailability(Long bookId);

    void increaseBookAvailability(Long bookId);

    boolean isBookAvailable(Long bookId);
}


