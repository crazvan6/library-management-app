package com.library.management.repository;

import com.library.management.entity.Loan;
import com.library.management.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByUser_UserIdAndStatus(Long userId, LoanStatus status);

    List<Loan> findByBook_BookIdAndStatus(Long bookId, LoanStatus status);

    List<Loan> findByStatus(LoanStatus status);

    Optional<Loan> findByReservation_ReservationId(Long reservationId);

    List<Loan> findByStatusAndDueDateBefore(LoanStatus status, LocalDateTime dueDate);

    @Query("SELECT l FROM Loan l WHERE l.user.userId = :userId AND l.status IN ('ACTIVE', 'OVERDUE') ORDER BY l.dueDate ASC")
    List<Loan> findActiveLoansForUser(@Param("userId") Long userId);

    @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE' AND l.dueDate < :now")
    List<Loan> findOverdueLoans(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.user.userId = :userId AND l.status IN ('ACTIVE', 'OVERDUE')")
    long countActiveLoansForUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.book.bookId = :bookId AND l.status IN ('ACTIVE', 'OVERDUE')")
    long countActiveLoansByBook(@Param("bookId") Long bookId);

    @Query("SELECT l FROM Loan l WHERE l.status IN ('ACTIVE', 'OVERDUE') ORDER BY l.dueDate ASC")
    List<Loan> findAllActiveLoans();

    @Query("SELECT l FROM Loan l WHERE l.user.userId = :userId ORDER BY l.checkoutDate DESC")
    List<Loan> findLoanHistoryForUser(@Param("userId") Long userId);
}


