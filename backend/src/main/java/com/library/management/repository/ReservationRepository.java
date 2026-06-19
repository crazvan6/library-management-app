package com.library.management.repository;

import com.library.management.entity.Book;
import com.library.management.entity.Reservation;
import com.library.management.entity.User;
import com.library.management.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserAndStatus(User user, ReservationStatus status);

    List<Reservation> findByUser_UserIdAndStatus(Long userId, ReservationStatus status);

    List<Reservation> findByBookAndStatus(Book book, ReservationStatus status);

    List<Reservation> findByBook_BookIdAndStatus(Long bookId, ReservationStatus status);

    Optional<Reservation> findByUserAndBookAndStatus(User user, Book book, ReservationStatus status);

    boolean existsByUserAndBookAndStatus(User user, Book book, ReservationStatus status);

    List<Reservation> findByStatusAndExpiryDateBefore(ReservationStatus status, LocalDateTime expiryDate);

    @Query("SELECT r FROM Reservation r WHERE r.user.userId = :userId AND r.status = 'PENDING' ORDER BY r.requestDate DESC")
    List<Reservation> findPendingReservationsByUser(@Param("userId") Long userId);

    @Query("SELECT r FROM Reservation r WHERE r.book.bookId = :bookId AND r.status = 'PENDING' ORDER BY r.requestDate ASC")
    List<Reservation> findPendingReservationsForBook(@Param("bookId") Long bookId);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.book.bookId = :bookId AND r.status = 'PENDING'")
    long countPendingReservationsForBook(@Param("bookId") Long bookId);

    @Query("SELECT r FROM Reservation r WHERE r.status = 'PENDING' AND r.expiryDate < :now")
    List<Reservation> findExpiredReservations(@Param("now") LocalDateTime now);

    List<Reservation> findByUser_UserIdOrderByRequestDateDesc(Long userId);
}

