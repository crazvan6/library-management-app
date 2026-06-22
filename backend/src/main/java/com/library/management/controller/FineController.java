package com.library.management.controller;

import com.library.management.dto.request.PayFineRequest;
import com.library.management.dto.request.WaiveFineRequest;
import com.library.management.dto.response.FineResponse;
import com.library.management.dto.response.FineSummaryResponse;
import com.library.management.dto.response.PageResponse;
import com.library.management.dto.response.UserFinesSummaryResponse;
import com.library.management.entity.User;
import com.library.management.exception.ForbiddenException;
import com.library.management.service.FineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/v1/fines")
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
@Validated
@RequiredArgsConstructor
public class FineController {

    private final FineService fineService;

    @GetMapping("/my-fines")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<UserFinesSummaryResponse> getMyFines(@AuthenticationPrincipal User user) {
        User current = requireUser(user);
        return ResponseEntity.ok(fineService.getMyFinesSummary(current.getUserId()));
    }

    @GetMapping("/my-fines/pending")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<FineResponse>> getMyPendingFines(@AuthenticationPrincipal User user) {
        User current = requireUser(user);
        return ResponseEntity.ok(fineService.getPendingFines(current.getUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FineResponse> getFineById(@PathVariable Long id, @AuthenticationPrincipal User user) {
        User current = requireUser(user);
        FineResponse fine = fineService.getFineById(id);
        boolean isOwner = fine.getUserId().equals(current.getUserId());
        boolean isStaff = current.isAdmin() || current.isLibrarian();
        if (!isOwner && !isStaff) {
            throw new ForbiddenException("You are not allowed to view this fine");
        }
        return ResponseEntity.ok(fine);
    }

    @GetMapping("/loan/{loanId}")
    public ResponseEntity<FineResponse> getFineByLoanId(@PathVariable Long loanId, @AuthenticationPrincipal User user) {
        User current = requireUser(user);
        FineResponse fine = fineService.getFineByLoanId(loanId);
        boolean isOwner = fine.getUserId().equals(current.getUserId());
        boolean isStaff = current.isAdmin() || current.isLibrarian();
        if (!isOwner && !isStaff) {
            throw new ForbiddenException("You are not allowed to view this fine");
        }
        return ResponseEntity.ok(fine);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('LIBRARIAN','ADMIN')")
    public ResponseEntity<List<FineResponse>> getAllPendingFines() {
        return ResponseEntity.ok(fineService.getAllPendingFines());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LIBRARIAN','ADMIN')")
    public ResponseEntity<PageResponse<FineSummaryResponse>> getFines(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(fineService.getFinesPage(pageable));
    }

    @PostMapping("/pay")
    @PreAuthorize("hasAnyRole('LIBRARIAN','ADMIN')")
    public ResponseEntity<FineResponse> payFine(@Valid @RequestBody PayFineRequest request,
                                                @AuthenticationPrincipal User librarian) {
        User current = requireUser(librarian);
        log.info("Librarian {} processing payment for fine {}", current.getUserId(), request.getFineId());
        return ResponseEntity.ok(fineService.payFine(request, current.getUserId()));
    }

    @PostMapping("/waive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FineResponse> waiveFine(@Valid @RequestBody WaiveFineRequest request,
                                                  @AuthenticationPrincipal User admin) {
        User current = requireUser(admin);
        log.info("Admin {} waiving fine {}", current.getUserId(), request.getFineId());
        return ResponseEntity.ok(fineService.waiveFine(request, current.getUserId()));
    }

    private User requireUser(User user) {
        if (user == null) {
            throw new com.library.management.exception.UnauthorizedException("No authenticated user");
        }
        return user;
    }
}


