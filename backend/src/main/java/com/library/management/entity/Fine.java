package com.library.management.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.library.management.enums.FineStatus;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fines", indexes = {
        @Index(name = "idx_fine_status", columnList = "status"),
        @Index(name = "idx_fine_user_status", columnList = "user_id, status")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Fine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fine_id")
    private Long fineId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false, unique = true)
    @JsonIgnore
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @DecimalMin("0.00")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Min(0)
    @Column(name = "days_overdue", nullable = false)
    private Integer daysOverdue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FineStatus status;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    @JsonIgnore
    private User processedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void pay(String paymentMethod, User processedBy) {
        if (!FineStatus.PENDING.equals(this.status)) {
            throw new InvalidOperationException("Fine is not pending");
        }
        this.status = FineStatus.PAID;
        this.paymentDate = LocalDateTime.now();
        this.paymentMethod = paymentMethod;
        this.processedBy = processedBy;
    }

    public void waive(String reason, User processedBy) {
        if (!FineStatus.PENDING.equals(this.status)) {
            throw new InvalidOperationException("Fine is not pending");
        }
        this.status = FineStatus.WAIVED;
        this.paymentDate = LocalDateTime.now();
        this.processedBy = processedBy;
        this.notes = reason;
    }

    public boolean isPending() {
        return FineStatus.PENDING.equals(this.status);
    }

    public boolean isResolved() {
        return this.status != null && this.status.isResolved();
    }

    public static BigDecimal calculateAmount(int daysOverdue, BigDecimal ratePerDay) {
        if (daysOverdue < 0) {
            throw new InvalidOperationException("Days overdue cannot be negative");
        }
        BigDecimal amount = ratePerDay.multiply(BigDecimal.valueOf(daysOverdue));
        return amount.max(BigDecimal.ZERO).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @PrePersist
    @PreUpdate
    public void validateAmount() {
        if (this.amount == null || this.amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidOperationException("Fine amount cannot be negative");
        }
    }
}

