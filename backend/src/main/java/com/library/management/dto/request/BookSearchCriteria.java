package com.library.management.dto.request;

import com.library.management.enums.BookStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.data.domain.Sort;

@Data
public class BookSearchCriteria {
    private String title;
    private String author;
    private String isbn;
    private Long categoryId;
    private BookStatus status;
    private Boolean availableOnly = false;
    private Integer minYear;
    private Integer maxYear;

    @Min(0)
    private Integer page = 0;

    @Min(1)
    @Max(100)
    private Integer size = 20;

    private String sortBy = "title";
    private String sortDirection = "ASC";

    public Sort.Direction sortDir() {
        return "DESC".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
    }
}


