package com.library.management.dto.request;

import com.library.management.enums.BookStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateBookRequest {
    @Size(max = 255)
    private String title;

    @Size(max = 255)
    private String author;

    @Min(1000)
    @Max(9999)
    private Integer publicationYear;

    @Min(0)
    private Integer quantity;

    private BookStatus status;

    private Set<Long> categoryIds;
}


