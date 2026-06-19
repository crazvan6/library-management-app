package com.library.management.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.library.management.enums.BookStatus;
import com.library.management.enums.LoanStatus;
import com.library.management.enums.ReservationStatus;
import com.library.management.exception.InvalidOperationException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "books", indexes = {
        @Index(name = "idx_book_title", columnList = "title"),
        @Index(name = "idx_book_author", columnList = "author"),
        @Index(name = "idx_book_isbn", columnList = "isbn"),
        @Index(name = "idx_book_status_available", columnList = "status, available_quantity")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"categories", "loans", "reservations"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    @EqualsAndHashCode.Include
    private Long bookId;

    @Column(nullable = false)
    @NotBlank
    @Size(max = 255)
    private String title;

    @Column(nullable = false)
    @NotBlank
    @Size(max = 255)
    private String author;

    @Column(unique = true, nullable = false, length = 20)
    @NotBlank
    @Size(max = 20)
    private String isbn;

    @Column(name = "publication_year")
    @Min(1000)
    @Max(9999)
    private Integer publicationYear;

    @Column(nullable = false)
    @Min(0)
    private Integer quantity;

    @Column(name = "available_quantity", nullable = false)
    @Min(0)
    private Integer availableQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookStatus status;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "book_categories",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @Builder.Default
    private Set<Category> categories = new HashSet<>();

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<Loan> loans = new ArrayList<>();

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<Reservation> reservations = new ArrayList<>();

    public boolean isAvailable() {
        return status != null && status.isAvailable() && availableQuantity != null && availableQuantity > 0;
    }

    public boolean hasStock() {
        return quantity != null && quantity > 0;
    }

    public void decreaseAvailableQuantity() {
        if (availableQuantity == null || availableQuantity <= 0) {
            throw new InvalidOperationException("No available copies to decrease");
        }
        availableQuantity -= 1;
        validateQuantities();
    }

    public void increaseAvailableQuantity() {
        if (availableQuantity == null) {
            availableQuantity = 0;
        }
        if (availableQuantity + 1 > quantity) {
            throw new InvalidOperationException("Available quantity cannot exceed total quantity");
        }
        availableQuantity += 1;
        validateQuantities();
    }

    public void addCategory(Category category) {
        categories.add(category);
        if (category.getBooks() != null) {
            category.getBooks().add(this);
        }
    }

    public void removeCategory(Category category) {
        categories.remove(category);
        if (category.getBooks() != null) {
            category.getBooks().remove(this);
        }
    }

    public void validateQuantities() {
        if (availableQuantity == null || quantity == null) {
            return;
        }
        if (availableQuantity < 0 || availableQuantity > quantity) {
            throw new InvalidOperationException("Available quantity must be between 0 and total quantity");
        }
    }

    public boolean hasActiveLoans() {
        return loans != null && loans.stream().anyMatch(loan -> LoanStatus.ACTIVE.equals(loan.getStatus()));
    }

    public long getPendingReservationsCount() {
        return reservations == null ? 0 : reservations.stream()
                .filter(r -> ReservationStatus.PENDING.equals(r.getStatus()))
                .count();
    }

    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = BookStatus.AVAILABLE;
        }
        if (availableQuantity == null) {
            availableQuantity = quantity;
        }
        validateQuantities();
    }

    @PreUpdate
    public void preUpdate() {
        validateQuantities();
    }
}

