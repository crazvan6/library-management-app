package com.library.management.service;

import com.library.management.dto.request.CreateReservationRequest;
import com.library.management.dto.response.ReservationResponse;
import com.library.management.entity.Book;
import com.library.management.entity.Reservation;
import com.library.management.entity.User;
import com.library.management.enums.BookStatus;
import com.library.management.enums.LoanStatus;
import com.library.management.enums.ReservationStatus;
import com.library.management.enums.UserRole;
import com.library.management.exception.BookNotAvailableException;
import com.library.management.exception.InvalidOperationException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.mapper.ReservationMapper;
import com.library.management.repository.BookRepository;
import com.library.management.repository.LoanRepository;
import com.library.management.repository.ReservationRepository;
import com.library.management.repository.UserRepository;
import com.library.management.service.impl.ReservationServiceImpl;
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
class ReservationServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LoanRepository loanRepository;

    private ReservationMapper reservationMapper;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    @BeforeEach
    void setup() {
        reservationMapper = new ReservationMapper();
        reservationService = new ReservationServiceImpl(reservationRepository, bookRepository, userRepository, loanRepository, reservationMapper);
        ReflectionTestUtils.setField(reservationService, "maxOutstandingFines", BigDecimal.TEN);
        ReflectionTestUtils.setField(reservationService, "holdHours", 48);
    }

    @Test
    void createReservation_happyPath() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setBookId(1L);

        User user = user(UserRole.STUDENT, true);
        Book book = book(1L, 1, 1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.hasOutstandingFinesAbove(1L, BigDecimal.TEN)).thenReturn(false);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(reservationRepository.existsByUserAndBookAndStatus(user, book, ReservationStatus.PENDING)).thenReturn(false);
        when(loanRepository.findByUser_UserIdAndStatus(1L, LoanStatus.ACTIVE)).thenReturn(Collections.emptyList());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse response = reservationService.createReservation(request, 1L);

        assertNotNull(response);
        assertEquals(0, book.getAvailableQuantity());
        verify(bookRepository).save(book);
    }

    @Test
    void createReservation_unavailableBook_throws() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setBookId(1L);
        User user = user(UserRole.STUDENT, true);
        Book book = book(1L, 1, 0);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.hasOutstandingFinesAbove(1L, BigDecimal.TEN)).thenReturn(false);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        assertThrows(BookNotAvailableException.class, () -> reservationService.createReservation(request, 1L));
    }

    @Test
    void cancelReservation_happyPath() {
        User user = user(UserRole.STUDENT, true);
        Book book = book(1L, 1, 0);
        Reservation reservation = Reservation.builder()
                .reservationId(5L)
                .user(user)
                .book(book)
                .status(ReservationStatus.PENDING)
                .requestDate(LocalDateTime.now())
                .expiryDate(LocalDateTime.now().plusHours(48))
                .build();

        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        reservationService.cancelReservation(5L, 1L);

        assertEquals(ReservationStatus.CANCELED, reservation.getStatus());
        assertEquals(1, book.getAvailableQuantity());
        verify(bookRepository).save(book);
    }

    @Test
    void getReservation_notFound_throws() {
        when(reservationRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> reservationService.getReservationById(99L));
    }

    private User user(UserRole role, boolean active) {
        return User.builder()
                .userId(1L)
                .email("u@test.com")
                .password("Password1!")
                .role(role)
                .isActive(active)
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
}


