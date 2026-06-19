package com.library.management.service.impl;

import com.library.management.dto.request.CheckoutRequest;
import com.library.management.dto.request.ReturnBookRequest;
import com.library.management.dto.response.CheckoutResponse;
import com.library.management.dto.response.FineResponse;
import com.library.management.dto.response.LoanResponse;
import com.library.management.dto.response.LoanSummaryResponse;
import com.library.management.dto.response.PageResponse;
import com.library.management.dto.response.ReturnBookResponse;
import com.library.management.entity.Book;
import com.library.management.entity.Loan;
import com.library.management.entity.Reservation;
import com.library.management.entity.User;
import com.library.management.enums.LoanStatus;
import com.library.management.enums.ReservationStatus;
import com.library.management.exception.BookNotAvailableException;
import com.library.management.exception.ForbiddenException;
import com.library.management.exception.InvalidOperationException;
import com.library.management.exception.ReservationExpiredException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.exception.UserNotEligibleException;
import com.library.management.mapper.LoanMapper;
import com.library.management.service.FineService;
import com.library.management.repository.BookRepository;
import com.library.management.repository.LoanRepository;
import com.library.management.repository.ReservationRepository;
import com.library.management.repository.UserRepository;
import com.library.management.service.LoanService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final LoanMapper loanMapper;
    private final FineService fineService;

    @Value("${library.fine.max-outstanding}")
    private BigDecimal maxOutstandingFines;

    @Value("${library.loan.duration-days:14}")
    private int loanDurationDays;

    @Override
    public CheckoutResponse checkout(CheckoutRequest request, Long librarianId) {
        User librarian = userRepository.findById(librarianId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", librarianId));
        if (!(librarian.isAdmin() || librarian.isLibrarian())) {
            throw new ForbiddenException("Only librarians can process checkouts");
        }

        User borrower = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));
        if (!Boolean.TRUE.equals(borrower.getIsActive())) {
            throw new UserNotEligibleException("Cannot checkout. User account is deactivated");
        }
        if (userRepository.hasOutstandingFinesAbove(request.getUserId(), maxOutstandingFines)) {
            throw new UserNotEligibleException("Cannot checkout. Outstanding fines exceed €" + maxOutstandingFines);
        }

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", request.getBookId()));

        Reservation reservation = null;
        if (request.getReservationId() != null) {
            reservation = reservationRepository.findById(request.getReservationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", request.getReservationId()));
            if (!reservation.getUser().getUserId().equals(request.getUserId())) {
                throw new InvalidOperationException("Reservation belongs to different user");
            }
            if (!reservation.getBook().getBookId().equals(request.getBookId())) {
                throw new InvalidOperationException("Reservation is for different book");
            }
            if (reservation.getStatus() != ReservationStatus.PENDING) {
                throw new InvalidOperationException("Reservation is not active (status: " + reservation.getStatus() + ")");
            }
            if (reservation.isExpired()) {
                throw new ReservationExpiredException("Reservation has expired");
            }
        } else {
            if (!book.isAvailable()) {
                throw new BookNotAvailableException("Book '" + book.getTitle() + "' has no available copies");
            }
        }

        long pendingReservations = reservationRepository.countPendingReservationsForBook(book.getBookId());
        if (reservation != null) {
            pendingReservations = Math.max(0, pendingReservations - 1);
        }

        Loan loan = loanMapper.toEntity(request, borrower, book, reservation, librarian, loanDurationDays, pendingReservations == 0);
        loanRepository.save(loan);

        if (reservation != null) {
            reservation.complete();
            reservationRepository.save(reservation);
        } else {
            book.decreaseAvailableQuantity();
            bookRepository.save(book);
        }

        LoanResponse loanResponse = loanMapper.toLoanResponse(loan);
        CheckoutResponse response = CheckoutResponse.builder()
                .loan(loanResponse)
                .message("Checkout successful")
                .success(true)
                .build();
        log.info("Checkout processed by librarian {} for user {} for book '{}' (ID: {})",
                librarianId, request.getUserId(), book.getTitle(), book.getBookId());
        return response;
    }

    @Override
    public LoanResponse getLoanById(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "id", loanId));
        return loanMapper.toLoanResponse(loan);
    }

    @Override
    public List<LoanResponse> getMyLoans(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return loanRepository.findLoanHistoryForUser(userId).stream()
                .map(loanMapper::toLoanResponse)
                .toList();
    }

    @Override
    public List<LoanResponse> getActiveLoans(Long userId) {
        return loanRepository.findActiveLoansForUser(userId).stream()
                .map(loanMapper::toLoanResponse)
                .toList();
    }

    @Override
    public List<LoanResponse> getAllActiveLoans() {
        return loanRepository.findAllActiveLoans().stream()
                .map(loanMapper::toLoanResponse)
                .toList();
    }

    @Override
    public List<LoanResponse> getOverdueLoans() {
        return loanRepository.findOverdueLoans(LocalDateTime.now()).stream()
                .map(loanMapper::toLoanResponse)
                .toList();
    }

    @Override
    public PageResponse<LoanSummaryResponse> getLoansPage(Pageable pageable) {
        return PageResponse.of(loanRepository.findAll(pageable).map(loanMapper::toLoanSummaryResponse));
    }

    @Override
    public LoanResponse getLoanByReservation(Long reservationId) {
        Loan loan = loanRepository.findByReservation_ReservationId(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "reservationId", reservationId));
        return loanMapper.toLoanResponse(loan);
    }

    @Override
    public void updateOverdueLoans() {
        List<Loan> overdue = loanRepository.findOverdueLoans(LocalDateTime.now());
        int count = 0;
        for (Loan loan : overdue) {
            if (loan.getStatus() == LoanStatus.ACTIVE) {
                loan.markAsOverdue();
                count++;
            }
        }
        loanRepository.saveAll(overdue);
        log.info("Updated {} loans to overdue status", count);
    }

    @Override
    public ReturnBookResponse returnBook(ReturnBookRequest request, Long librarianId) {
        User librarian = userRepository.findById(librarianId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", librarianId));
        if (!(librarian.isAdmin() || librarian.isLibrarian())) {
            throw new ForbiddenException("Only librarians can process returns");
        }

        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "id", request.getLoanId()));
        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new InvalidOperationException("This book has already been returned");
        }

        loan.returnBook(librarian);
        loanRepository.save(loan);

        loan.getBook().increaseAvailableQuantity();
        bookRepository.save(loan.getBook());

        boolean wasOverdue = loan.getDaysOverdue() > 0;
        FineResponse fineResponse = null;
        if (wasOverdue) {
            fineResponse = fineService.createFine(loan.getLoanId(), (int) loan.getDaysOverdue());
        }

        ReturnBookResponse response = ReturnBookResponse.builder()
                .loanId(loan.getLoanId())
                .bookTitle(loan.getBook().getTitle())
                .returnDate(loan.getReturnDate())
                .wasOverdue(wasOverdue)
                .fine(fineResponse)
                .message(wasOverdue ? "Book returned with fine created" : "Book returned successfully")
                .build();

        log.info("Book returned for loan {} by librarian {}. Days overdue: {}, Fine: {}",
                loan.getLoanId(), librarianId, wasOverdue ? loan.getDaysOverdue() : 0,
                fineResponse != null ? fineResponse.getAmount() : BigDecimal.ZERO);
        return response;
    }
}

