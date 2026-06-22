package com.library.management.dto.response;

import com.library.management.enums.BookStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookSummaryResponse {
    private Long bookId;
    private String title;
    private String author;
    private String isbn;
    private Integer availableQuantity;
    private BookStatus status;
    private List<String> categoryNames;
    private Boolean isAvailable;
}


