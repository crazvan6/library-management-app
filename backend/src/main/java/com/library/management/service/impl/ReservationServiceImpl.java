package com.library.management.service.impl;

import com.library.management.dto.request.CreateReservationRequest;
import com.library.management.dto.response.ReservationResponse;
import com.library.management.entity.Book;
import com.library.management.entity.Reservation;
import com.library.management.entity.User;
import com.library.management.enums.LoanStatus;
import com.library.management.enums.ReservationStatus;
import com.library.management.exception.BookNotAvailableException;
import com.library.management.exception.ForbiddenException;
import com.library.management.exception.InvalidOperationException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.exception.UserNotEligibleException;
import com.library.management.mapper.ReservationMapper;
import com.library.management.repository.BookRepository;
import com.library.management.repository.LoanRepository;
import com.library.management.repository.ReservationRepository;
import com.library.management.repository.UserRepository;
import com.library.management.service.ReservationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final LoanRepository loanRepository;
    private final ReservationMapper reservationMapper;

    @Value("${library.fine.max-outstanding}")
    private BigDecimal maxOutstandingFines;

    @Value("${library.reservation.hold-hours:48}")
    private int holdHours;

    @Override
    public ReservationResponse createReservation(CreateReservationRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (!user.isStudent()) {
            throw new ForbiddenException("Only students can create reservations");
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new UserNotEligibleException("Your account is deactivated");
        }
        if (userRepository.hasOutstandingFinesAbove(userId, maxOutstandingFines)) {
            throw new UserNotEligibleException("Cannot create reservation. Outstanding fines exceed €" + maxOutstandingFines);
        }

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", request.getBookId()));
        if (!book.isAvailable()) {
            throw new BookNotAvailableException("Book '" + book.getTitle() + "' is not available for reservation");
        }

        if (reservationRepository.existsByUserAndBookAndStatus(user, book, ReservationStatus.PENDING)) {
            throw new InvalidOperationException("You already have an active reservation for this book");
        }

        boolean alreadyLoaned = loanRepository.findByUser_UserIdAndStatus(userId, LoanStatus.ACTIVE).stream()
                .anyMatch(l -> l.getBook().getBookId().equals(book.getBookId()));
        if (alreadyLoaned) {
            throw new InvalidOperationException("You already have this book checked out");
        }

        Reservation reservation = reservationMapper.toEntity(request, user, book);
        reservation.setExpiryDate(reservation.getRequestDate().plusHours(holdHours));

        reservationRepository.save(reservation);
        book.decreaseAvailableQuantity();
        bookRepository.save(book);

        ReservationResponse response = reservationMapper.toReservationResponse(reservation);
        response.setQueuePosition(getQueuePosition(reservation.getReservationId()));
        log.info("Reservation created for user {} for book '{}' (ID: {})", userId, book.getTitle(), book.getBookId());
        return response;
    }

    @Override
    public void cancelReservation(Long reservationId, Long userId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", reservationId));
        if (!reservation.getUser().getUserId().equals(userId)) {
            throw new ForbiddenException("You can only cancel your own reservations");
        }
        if (!reservation.canBeCanceled()) {
            throw new InvalidOperationException("This reservation cannot be canceled (status: " + reservation.getStatus() + ")");
        }
        reservation.cancel();
        reservationRepository.save(reservation);
        reservation.getBook().increaseAvailableQuantity();
        bookRepository.save(reservation.getBook());
        log.info("Reservation {} canceled by user {}", reservationId, userId);
    }

    @Override
    public ReservationResponse getReservationById(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", reservationId));
        ReservationResponse response = reservationMapper.toReservationResponse(reservation);
        if (ReservationStatus.PENDING.equals(reservation.getStatus())) {
            response.setQueuePosition(getQueuePosition(reservationId));
        }
        return response;
    }

    @Override
    public List<ReservationResponse> getMyReservations(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return reservationRepository.findByUser_UserIdOrderByRequestDateDesc(userId).stream()
                .map(reservation -> {
                    ReservationResponse resp = reservationMapper.toReservationResponse(reservation);
                    if (ReservationStatus.PENDING.equals(reservation.getStatus())) {
                        resp.setQueuePosition(getQueuePosition(reservation.getReservationId()));
                    }
                    return resp;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ReservationResponse> getPendingReservations(Long userId) {
        return reservationRepository.findPendingReservationsByUser(userId).stream()
                .map(reservation -> {
                    ReservationResponse resp = reservationMapper.toReservationResponse(reservation);
                    resp.setQueuePosition(getQueuePosition(reservation.getReservationId()));
                    return resp;
                })
                .toList();
    }

    @Override
    public List<ReservationResponse> getReservationsForBook(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new ResourceNotFoundException("Book", "id", bookId);
        }
        return reservationRepository.findPendingReservationsForBook(bookId).stream()
                .map(reservation -> {
                    ReservationResponse resp = reservationMapper.toReservationResponse(reservation);
                    resp.setQueuePosition(getQueuePosition(reservation.getReservationId()));
                    return resp;
                })
                .toList();
    }

    @Override
    public int getQueuePosition(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if (reservation == null || reservation.getStatus() != ReservationStatus.PENDING) {
            return -1;
        }
        List<Reservation> queue = reservationRepository.findPendingReservationsForBook(reservation.getBook().getBookId());
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getReservationId().equals(reservationId)) {
                return i + 1;
            }
        }
        return -1;
    }

    @Override
    public void expireReservations() {
        List<Reservation> expired = reservationRepository.findExpiredReservations(LocalDateTime.now());
        int count = 0;
        for (Reservation reservation : expired) {
            try {
                reservation.expire();
                reservation.getBook().increaseAvailableQuantity();
                bookRepository.save(reservation.getBook());
                count++;
            } catch (Exception ignored) {
                // ignore invalid transitions
            }
        }
        reservationRepository.saveAll(expired);
        log.info("Expired {} reservations", count);
    }
}

