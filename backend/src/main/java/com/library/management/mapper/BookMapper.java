package com.library.management.mapper;

import com.library.management.dto.request.CreateBookRequest;
import com.library.management.dto.request.UpdateBookRequest;
import com.library.management.dto.response.BookResponse;
import com.library.management.dto.response.BookSummaryResponse;
import com.library.management.dto.response.CategoryResponse;
import com.library.management.entity.Book;
import com.library.management.entity.Category;
import com.library.management.enums.BookStatus;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class BookMapper {

    public BookResponse toBookResponse(Book book) {
        if (book == null) {
            return null;
        }
        Set<CategoryResponse> categories = book.getCategories().stream()
                .map(c -> CategoryResponse.builder()
                        .categoryId(c.getCategoryId())
                        .name(c.getName())
                        .description(c.getDescription())
                        .bookCount(c.getBookCount())
                        .createdAt(c.getCreatedAt())
                        .build())
                .collect(Collectors.toSet());

        return BookResponse.builder()
                .bookId(book.getBookId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .publicationYear(book.getPublicationYear())
                .quantity(book.getQuantity())
                .availableQuantity(book.getAvailableQuantity())
                .status(book.getStatus())
                .categories(categories)
                .isAvailable(book.isAvailable())
                .createdAt(book.getCreatedAt())
                .updatedAt(book.getUpdatedAt())
                .build();
    }

    public BookSummaryResponse toBookSummaryResponse(Book book) {
        return BookSummaryResponse.builder()
                .bookId(book.getBookId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .availableQuantity(book.getAvailableQuantity())
                .status(book.getStatus())
                .categoryNames(book.getCategories().stream().map(Category::getName).toList())
                .isAvailable(book.isAvailable())
                .build();
    }

    public Book toEntity(CreateBookRequest request) {
        return Book.builder()
                .title(request.getTitle())
                .author(request.getAuthor())
                .isbn(request.getIsbn())
                .publicationYear(request.getPublicationYear())
                .quantity(request.getQuantity())
                .availableQuantity(request.getQuantity())
                .status(BookStatus.AVAILABLE)
                .build();
    }

    public void updateEntity(Book book, UpdateBookRequest request) {
        if (request.getTitle() != null) {
            book.setTitle(request.getTitle());
        }
        if (request.getAuthor() != null) {
            book.setAuthor(request.getAuthor());
        }
        if (request.getPublicationYear() != null) {
            book.setPublicationYear(request.getPublicationYear());
        }
        if (request.getQuantity() != null) {
            book.setQuantity(request.getQuantity());
        }
        if (request.getStatus() != null) {
            book.setStatus(request.getStatus());
        }
    }
}


