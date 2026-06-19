package com.library.management.controller;

import com.library.management.dto.request.CheckoutRequest;
import com.library.management.dto.request.ReturnBookRequest;
import com.library.management.dto.response.CheckoutResponse;
import com.library.management.dto.response.LoanResponse;
import com.library.management.dto.response.ReturnBookResponse;
import com.library.management.entity.User;
import com.library.management.exception.ForbiddenException;
import com.library.management.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/loans")
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
@Validated
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping("/checkout")
    @PreAuthorize("hasAnyRole('LIBRARIAN','ADMIN')")
    public ResponseEntity<CheckoutResponse> checkout(@Valid @RequestBody CheckoutRequest request,
                                                     @AuthenticationPrincipal User librarian) {
        User current = requireUser(librarian);
        log.info("Librarian {} processing checkout for user {} for book {}", current.getUserId(), request.getUserId(), request.getBookId());
        CheckoutResponse response = loanService.checkout(request, current.getUserId());
        log.info("Checkout completed successfully. Loan ID: {}", response.getLoan().getLoanId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-loans")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<LoanResponse>> getMyLoans(@AuthenticationPrincipal User user) {
        User current = requireUser(user);
        return ResponseEntity.ok(loanService.getMyLoans(current.getUserId()));
    }

    @GetMapping("/my-active-loans")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<LoanResponse>> getMyActiveLoans(@AuthenticationPrincipal User user) {
        User current = requireUser(user);
        return ResponseEntity.ok(loanService.getActiveLoans(current.getUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanResponse> getLoanById(@PathVariable Long id, @AuthenticationPrincipal User user) {
        LoanResponse response = loanService.getLoanById(id);
        User current = requireUser(user);
        boolean isOwner = response.getUserId().equals(current.getUserId());
        boolean isStaff = current.isAdmin() || current.isLibrarian();
        if (!isOwner && !isStaff) {
            throw new ForbiddenException("You are not allowed to view this loan");
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('LIBRARIAN','ADMIN')")
    public ResponseEntity<List<LoanResponse>> getAllActiveLoans() {
        return ResponseEntity.ok(loanService.getAllActiveLoans());
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('LIBRARIAN','ADMIN')")
    public ResponseEntity<List<LoanResponse>> getOverdueLoans() {
        return ResponseEntity.ok(loanService.getOverdueLoans());
    }

    @GetMapping("/reservation/{reservationId}")
    @PreAuthorize("hasAnyRole('LIBRARIAN','ADMIN')")
    public ResponseEntity<LoanResponse> getLoanByReservation(@PathVariable Long reservationId) {
        return ResponseEntity.ok(loanService.getLoanByReservation(reservationId));
    }

    @PostMapping("/return")
    @PreAuthorize("hasAnyRole('LIBRARIAN','ADMIN')")
    public ResponseEntity<ReturnBookResponse> returnBook(@Valid @RequestBody ReturnBookRequest request,
                                                         @AuthenticationPrincipal User librarian) {
        User current = requireUser(librarian);
        log.info("Librarian {} processing return for loan {}", current.getUserId(), request.getLoanId());
        ReturnBookResponse response = loanService.returnBook(request, current.getUserId());
        log.info("Return processed. Loan: {}, Overdue: {}, Fine: {}",
                response.getLoanId(), response.getWasOverdue(),
                response.getFine() != null ? response.getFine().getAmount() : null);
        return ResponseEntity.ok(response);
    }

    private User requireUser(User user) {
        if (user == null) {
            throw new com.library.management.exception.UnauthorizedException("No authenticated user");
        }
        return user;
    }
}

