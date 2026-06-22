package com.library.management.dto.response;

import com.library.management.enums.BookStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookResponse {
    private Long bookId;
    private String title;
    private String author;
    private String isbn;
    private Integer publicationYear;
    private Integer quantity;
    private Integer availableQuantity;
    private BookStatus status;
    private Set<CategoryResponse> categories;
    private Boolean isAvailable;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


