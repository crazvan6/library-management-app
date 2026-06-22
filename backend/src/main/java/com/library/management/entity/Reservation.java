package com.library.management.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "reservations", indexes = {
        @Index(name = "idx_reservation_status", columnList = "status"),
        @Index(name = "idx_reservation_expiry", columnList = "status, expiry_date"),
        @Index(name = "idx_reservation_user_status", columnList = "user_id, status"),
        @Index(name = "idx_reservation_book_status", columnList = "book_id, status")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long reservationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    @JsonIgnore
    private Book book;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(name = "request_date", nullable = false)
    private LocalDateTime requestDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "completed_date")
    private LocalDateTime completedDate;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "reservation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Loan loan;

    public boolean isExpired() {
        return ReservationStatus.PENDING.equals(status) && expiryDate != null && LocalDateTime.now().isAfter(expiryDate);
    }

    public boolean canBeCanceled() {
        return status != null && status.canBeCanceled() && !isExpired();
    }

    public void cancel() {
        if (!canBeCanceled()) {
            throw new InvalidOperationException("Reservation cannot be canceled");
        }
        this.status = ReservationStatus.CANCELED;
    }

    public void complete() {
        this.status = ReservationStatus.COMPLETED;
        this.completedDate = LocalDateTime.now();
    }

    public void expire() {
        if (!isExpired()) {
            throw new InvalidOperationException("Reservation is not expired");
        }
        this.status = ReservationStatus.EXPIRED;
    }

    public long getHoursUntilExpiry() {
        return expiryDate == null ? 0 : ChronoUnit.HOURS.between(LocalDateTime.now(), expiryDate);
    }

    @PrePersist
    public void setExpiryDate() {
        if (this.requestDate == null) {
            this.requestDate = LocalDateTime.now();
        }
        if (this.expiryDate == null) {
            this.expiryDate = this.requestDate.plusHours(48);
        }
        if (this.status == null) {
            this.status = ReservationStatus.PENDING;
        }
    }
}

