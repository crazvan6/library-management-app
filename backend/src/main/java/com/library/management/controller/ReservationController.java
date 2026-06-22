package com.library.management.controller;

import com.library.management.dto.request.CreateReservationRequest;
import com.library.management.dto.response.MessageResponse;
import com.library.management.dto.response.ReservationResponse;
import com.library.management.entity.User;
import com.library.management.exception.ForbiddenException;
import com.library.management.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/reservations")
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
@Validated
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping("/")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ReservationResponse> createReservation(@Valid @RequestBody CreateReservationRequest request,
                                                                 @AuthenticationPrincipal User user) {
        log.info("User {} creating reservation for book {}", user.getUserId(), request.getBookId());
        ReservationResponse response = reservationService.createReservation(request, user.getUserId());
        log.info("Reservation {} created successfully", response.getReservationId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-reservations")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(@AuthenticationPrincipal User user) {
        User current = requireUser(user);
        return ResponseEntity.ok(reservationService.getMyReservations(current.getUserId()));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<ReservationResponse>> getMyPendingReservations(@AuthenticationPrincipal User user) {
        User current = requireUser(user);
        return ResponseEntity.ok(reservationService.getPendingReservations(current.getUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservationById(@PathVariable Long id,
                                                                  @AuthenticationPrincipal User user) {
        ReservationResponse response = reservationService.getReservationById(id);
        User current = requireUser(user);
        boolean isOwner = response.getUserId().equals(current.getUserId());
        boolean isStaff = current.isAdmin() || current.isLibrarian();
        if (!isOwner && !isStaff) {
            throw new ForbiddenException("You are not allowed to view this reservation");
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<MessageResponse> cancelReservation(@PathVariable Long id,
                                                             @AuthenticationPrincipal User user) {
        User current = requireUser(user);
        log.info("User {} canceling reservation {}", current.getUserId(), id);
        reservationService.cancelReservation(id, current.getUserId());
        return ResponseEntity.ok(MessageResponse.builder().message("Reservation canceled successfully").build());
    }

    @GetMapping("/book/{bookId}")
    @PreAuthorize("hasAnyRole('LIBRARIAN','ADMIN')")
    public ResponseEntity<List<ReservationResponse>> getReservationsForBook(@PathVariable Long bookId) {
        log.info("Fetching reservations for book {}", bookId);
        return ResponseEntity.ok(reservationService.getReservationsForBook(bookId));
    }

    private User requireUser(User user) {
        if (user == null) {
            throw new com.library.management.exception.UnauthorizedException("No authenticated user");
        }
        return user;
    }
}

