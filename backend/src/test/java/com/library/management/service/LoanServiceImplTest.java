package com.library.management.service;

import com.library.management.dto.request.CheckoutRequest;
import com.library.management.dto.request.ReturnBookRequest;
import com.library.management.dto.response.CheckoutResponse;
import com.library.management.dto.response.ReturnBookResponse;
import com.library.management.entity.Book;
import com.library.management.entity.Loan;
import com.library.management.entity.Reservation;
import com.library.management.entity.User;
import com.library.management.enums.BookStatus;
import com.library.management.enums.LoanStatus;
import com.library.management.enums.ReservationStatus;
import com.library.management.enums.UserRole;
import com.library.management.exception.BookNotAvailableException;
import com.library.management.exception.InvalidOperationException;
import com.library.management.exception.ReservationExpiredException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.mapper.LoanMapper;
import com.library.management.repository.BookRepository;
import com.library.management.repository.LoanRepository;
import com.library.management.repository.ReservationRepository;
import com.library.management.repository.UserRepository;
import com.library.management.service.impl.LoanServiceImpl;
import com.library.management.service.FineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceImplTest {

    @Mock
    private LoanRepository loanRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FineService fineService;

    private LoanMapper loanMapper;

    @InjectMocks
    private LoanServiceImpl loanService;

    @BeforeEach
    void setup() {
        loanMapper = new LoanMapper();
        loanService = new LoanServiceImpl(loanRepository, reservationRepository, bookRepository, userRepository, loanMapper, fineService);
        ReflectionTestUtils.setField(loanService, "maxOutstandingFines", BigDecimal.TEN);
        ReflectionTestUtils.setField(loanService, "loanDurationDays", 14);
    }

    @Test
    void checkout_withReservation_success() {
        CheckoutRequest request = checkoutRequest(1L, 10L, 100L);
        User librarian = user(2L, UserRole.LIBRARIAN);
        User borrower = user(1L, UserRole.STUDENT);
        Book book = book(10L, 1, 0);

        Reservation reservation = Reservation.builder()
                .reservationId(100L)
                .user(borrower)
                .book(book)
                .status(ReservationStatus.PENDING)
                .requestDate(LocalDateTime.now().minusHours(1))
                .expiryDate(LocalDateTime.now().plusHours(47))
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(librarian));
        when(userRepository.findById(1L)).thenReturn(Optional.of(borrower));
        when(userRepository.hasOutstandingFinesAbove(1L, BigDecimal.TEN)).thenReturn(false);
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.countPendingReservationsForBook(10L)).thenReturn(1L);
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        CheckoutResponse response = loanService.checkout(request, 2L);

        assertNotNull(response.getLoan());
        assertEquals(LoanStatus.ACTIVE, response.getLoan().getStatus());
        assertEquals(ReservationStatus.COMPLETED, reservation.getStatus());
        verify(reservationRepository).save(reservation);
        verify(bookRepository, never()).save(book); // availability unchanged (already reserved)
    }

    @Test
    void checkout_withoutReservation_unavailableBookThrows() {
        CheckoutRequest request = checkoutRequest(1L, 10L, null);
        User librarian = user(2L, UserRole.LIBRARIAN);
        User borrower = user(1L, UserRole.STUDENT);
        Book book = book(10L, 0, 0);

        when(userRepository.findById(2L)).thenReturn(Optional.of(librarian));
        when(userRepository.findById(1L)).thenReturn(Optional.of(borrower));
        when(userRepository.hasOutstandingFinesAbove(1L, BigDecimal.TEN)).thenReturn(false);
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

        assertThrows(BookNotAvailableException.class, () -> loanService.checkout(request, 2L));
    }

    @Test
    void checkout_expiredReservationThrows() {
        CheckoutRequest request = checkoutRequest(1L, 10L, 100L);
        User librarian = user(2L, UserRole.LIBRARIAN);
        User borrower = user(1L, UserRole.STUDENT);
        Book book = book(10L, 1, 0);

        Reservation reservation = Reservation.builder()
                .reservationId(100L)
                .user(borrower)
                .book(book)
                .status(ReservationStatus.PENDING)
                .requestDate(LocalDateTime.now().minusHours(50))
                .expiryDate(LocalDateTime.now().minusHours(1))
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(librarian));
        when(userRepository.findById(1L)).thenReturn(Optional.of(borrower));
        when(userRepository.hasOutstandingFinesAbove(1L, BigDecimal.TEN)).thenReturn(false);
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        assertThrows(ReservationExpiredException.class, () -> loanService.checkout(request, 2L));
    }

    @Test
    void updateOverdueLoans_marksActiveAsOverdue() {
        Loan loan = Loan.builder()
                .loanId(5L)
                .status(LoanStatus.ACTIVE)
                .dueDate(LocalDateTime.now().minusDays(1))
                .build();
        when(loanRepository.findOverdueLoans(any(LocalDateTime.class))).thenReturn(List.of(loan));

        loanService.updateOverdueLoans();

        assertEquals(LoanStatus.OVERDUE, loan.getStatus());
        verify(loanRepository).saveAll(anyList());
    }

    @Test
    void getLoanByReservation_notFoundThrows() {
        when(loanRepository.findByReservation_ReservationId(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> loanService.getLoanByReservation(1L));
    }

    @Test
    void returnBook_overdueLoan_createsFine() {
        User librarian = user(2L, UserRole.LIBRARIAN);
        Book book = book(10L, 1, 0);
        Loan loan = Loan.builder()
                .loanId(7L)
                .user(user(1L, UserRole.STUDENT))
                .book(book)
                .status(LoanStatus.ACTIVE)
                .dueDate(LocalDateTime.now().minusDays(5).minusHours(1)) // 5 full days late
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(librarian));
        when(loanRepository.findById(7L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(fineService.createFine(7L, 5)).thenReturn(null);

        ReturnBookResponse response = loanService.returnBook(returnRequest(7L), 2L);

        assertTrue(response.getWasOverdue());
        assertEquals(LoanStatus.RETURNED, loan.getStatus());
        verify(fineService).createFine(7L, 5); // the bug previously never called this
    }

    @Test
    void returnBook_onTimeLoan_noFine() {
        User librarian = user(2L, UserRole.LIBRARIAN);
        Book book = book(10L, 1, 0);
        Loan loan = Loan.builder()
                .loanId(8L)
                .user(user(1L, UserRole.STUDENT))
                .book(book)
                .status(LoanStatus.ACTIVE)
                .dueDate(LocalDateTime.now().plusDays(3)) // not due yet
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(librarian));
        when(loanRepository.findById(8L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        ReturnBookResponse response = loanService.returnBook(returnRequest(8L), 2L);

        assertFalse(response.getWasOverdue());
        verify(fineService, never()).createFine(anyLong(), anyInt());
    }

    private CheckoutRequest checkoutRequest(Long userId, Long bookId, Long reservationId) {
        CheckoutRequest request = new CheckoutRequest();
        request.setUserId(userId);
        request.setBookId(bookId);
        request.setReservationId(reservationId);
        return request;
    }

    private User user(Long id, UserRole role) {
        return User.builder()
                .userId(id)
                .email("u@test.com")
                .password("Password1!")
                .role(role)
                .isActive(true)
                .build();
    }

    private Book book(Long id, int quantity, int available) {
        return Book.builder()
                .bookId(id)
                .title("t")
                .author("a")
                .isbn("123")
                .quantity(quantity)
                .availableQuantity(available)
                .status(BookStatus.AVAILABLE)
                .build();
    }

    private ReturnBookRequest returnRequest(Long loanId) {
        ReturnBookRequest request = new ReturnBookRequest();
        request.setLoanId(loanId);
        return request;
    }
}

