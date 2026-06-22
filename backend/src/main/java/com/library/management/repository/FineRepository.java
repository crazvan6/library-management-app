package com.library.management.repository;

import com.library.management.entity.Fine;
import com.library.management.enums.FineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FineRepository extends JpaRepository<Fine, Long> {

    Optional<Fine> findByLoan_LoanId(Long loanId);

    List<Fine> findByUser_UserId(Long userId);

    List<Fine> findByUser_UserIdAndStatus(Long userId, FineStatus status);

    List<Fine> findByStatus(FineStatus status);

    boolean existsByLoan_LoanId(Long loanId);

    @Query("SELECT f FROM Fine f WHERE f.user.userId = :userId AND f.status = 'PENDING' ORDER BY f.createdAt DESC")
    List<Fine> findPendingFinesByUser(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(f.amount), 0) FROM Fine f WHERE f.user.userId = :userId AND f.status = 'PENDING'")
    BigDecimal getTotalOutstandingFinesByUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM Fine f WHERE f.user.userId = :userId AND f.status = :status")
    long countByUserAndStatus(@Param("userId") Long userId, @Param("status") FineStatus status);

    @Query("SELECT f FROM Fine f WHERE f.status = 'PENDING' ORDER BY f.amount DESC")
    List<Fine> findAllPendingFines();

    @Query("SELECT f FROM Fine f WHERE f.status IN ('PAID', 'WAIVED') AND f.paymentDate BETWEEN :startDate AND :endDate")
    List<Fine> findResolvedFinesBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}


