package com.library.management.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.library.management.enums.LoanStatus;
import com.library.management.exception.InvalidOperationException;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "loans", indexes = {
        @Index(name = "idx_loan_status", columnList = "status"),
        @Index(name = "idx_loan_due_date", columnList = "due_date"),
        @Index(name = "idx_loan_return", columnList = "return_date"),
        @Index(name = "idx_loan_user_status", columnList = "user_id, status"),
        @Index(name = "idx_loan_status_due", columnList = "status, due_date")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loan_id")
    private Long loanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    @JsonIgnore
    private Book book;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    @JsonIgnore
    private Reservation reservation;

    @Column(name = "checkout_date", nullable = false)
    private LocalDateTime checkoutDate;

    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    @Column(name = "return_date")
    private LocalDateTime returnDate;

    @Column(name = "renewal_count", nullable = false)
    private Integer renewalCount;

    @Column(name = "is_renewable", nullable = false)
    private Boolean isRenewable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_out_by")
    @JsonIgnore
    private User checkedOutBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "returned_by")
    @JsonIgnore
    private User returnedBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "loan", cascade = jakarta.persistence.CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Fine fine;

    public boolean isOverdue() {
        return LoanStatus.ACTIVE.equals(status) && dueDate != null && LocalDateTime.now().isAfter(dueDate);
    }

    public boolean isActive() {
        return status != null && status.isActive();
    }

    public boolean canBeRenewed() {
        return Boolean.TRUE.equals(isRenewable) && status == LoanStatus.ACTIVE && !isOverdue() && renewalCount < 1;
    }

    public void renew(int extensionDays) {
        if (!canBeRenewed()) {
            throw new InvalidOperationException("Loan cannot be renewed");
        }
        this.dueDate = this.dueDate.plusDays(extensionDays);
        this.renewalCount = this.renewalCount + 1;
    }

    public void returnBook(User librarian) {
        this.returnDate = LocalDateTime.now();
        this.returnedBy = librarian;
        this.status = LoanStatus.RETURNED;
    }

    public void markAsOverdue() {
        this.status = LoanStatus.OVERDUE;
    }

    public long getDaysOverdue() {
        if (!isOverdue()) {
            return 0;
        }
        return ChronoUnit.DAYS.between(dueDate, LocalDateTime.now());
    }

    public long getDaysUntilDue() {
        if (dueDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(LocalDateTime.now(), dueDate);
    }

    @PrePersist
    public void setDefaults() {
        if (this.checkoutDate == null) {
            this.checkoutDate = LocalDateTime.now();
        }
        if (this.dueDate == null) {
            this.dueDate = this.checkoutDate.plusDays(14);
        }
        if (this.renewalCount == null) {
            this.renewalCount = 0;
        }
        if (this.isRenewable == null) {
            this.isRenewable = true;
        }
        if (this.status == null) {
            this.status = LoanStatus.ACTIVE;
        }
        if (this.renewalCount < 0 || this.renewalCount > 1) {
            throw new InvalidOperationException("Invalid renewal count");
        }
    }
}

