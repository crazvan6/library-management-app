package com.library.management.service.impl;

import com.library.management.dto.request.PayFineRequest;
import com.library.management.dto.request.WaiveFineRequest;
import com.library.management.dto.response.FineResponse;
import com.library.management.dto.response.FineSummaryResponse;
import com.library.management.dto.response.UserFinesSummaryResponse;
import com.library.management.entity.Fine;
import com.library.management.entity.Loan;
import com.library.management.entity.User;
import com.library.management.enums.FineStatus;
import com.library.management.enums.LoanStatus;
import com.library.management.exception.DuplicateResourceException;
import com.library.management.exception.ForbiddenException;
import com.library.management.exception.InvalidOperationException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.mapper.FineMapper;
import com.library.management.repository.FineRepository;
import com.library.management.repository.LoanRepository;
import com.library.management.repository.UserRepository;
import com.library.management.service.FineService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class FineServiceImpl implements FineService {

    private final FineRepository fineRepository;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final FineMapper fineMapper;

    @Value("${library.fine.rate-per-day:0.5}")
    private BigDecimal fineRatePerDay;

    @Value("${library.fine.max-outstanding:10}")
    private BigDecimal maxOutstandingFines;

    @Override
    public FineResponse createFine(Long loanId, int daysOverdue) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "id", loanId));
        if (fineRepository.existsByLoan_LoanId(loanId)) {
            throw new DuplicateResourceException("Fine", "loanId", loanId.toString());
        }
        if (daysOverdue < 0) {
            throw new InvalidOperationException("Days overdue cannot be negative");
        }
        BigDecimal amount = Fine.calculateAmount(daysOverdue, fineRatePerDay);
        Fine fine = fineMapper.toEntity(loan, loan.getUser(), daysOverdue, amount);
        fineRepository.save(fine);
        log.info("Fine created for loan {} (user: {}): €{} for {} days overdue", loanId, loan.getUser().getUserId(), amount, daysOverdue);
        return fineMapper.toFineResponse(fine);
    }

    @Override
    public FineResponse getFineById(Long fineId) {
        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new ResourceNotFoundException("Fine", "id", fineId));
        return fineMapper.toFineResponse(fine);
    }

    @Override
    public FineResponse getFineByLoanId(Long loanId) {
        Fine fine = fineRepository.findByLoan_LoanId(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Fine", "loanId", loanId));
        return fineMapper.toFineResponse(fine);
    }

    @Override
    public List<FineResponse> getMyFines(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }
        return fineRepository.findByUser_UserId(userId).stream()
                .map(fineMapper::toFineResponse)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    public List<FineResponse> getPendingFines(Long userId) {
        return fineRepository.findPendingFinesByUser(userId).stream()
                .map(fineMapper::toFineResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserFinesSummaryResponse getMyFinesSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        BigDecimal totalOutstanding = fineRepository.getTotalOutstandingFinesByUser(userId);
        long pendingCount = fineRepository.countByUserAndStatus(userId, FineStatus.PENDING);
        long paidCount = fineRepository.countByUserAndStatus(userId, FineStatus.PAID);
        long waivedCount = fineRepository.countByUserAndStatus(userId, FineStatus.WAIVED);

        List<FineSummaryResponse> fines = fineRepository.findByUser_UserId(userId).stream()
                .map(fineMapper::toFineSummaryResponse)
                .collect(Collectors.toList());

        return UserFinesSummaryResponse.builder()
                .userId(userId)
                .userFullName(user.getFullName())
                .totalOutstandingFines(totalOutstanding)
                .pendingFinesCount((int) pendingCount)
                .paidFinesCount((int) paidCount)
                .waivedFinesCount((int) waivedCount)
                .canBorrow(totalOutstanding.compareTo(maxOutstandingFines) < 0)
                .fines(fines)
                .build();
    }

    @Override
    public List<FineResponse> getAllPendingFines() {
        return fineRepository.findAllPendingFines().stream()
                .map(fineMapper::toFineResponse)
                .collect(Collectors.toList());
    }

    @Override
    public FineResponse payFine(PayFineRequest request, Long librarianId) {
        Fine fine = fineRepository.findById(request.getFineId())
                .orElseThrow(() -> new ResourceNotFoundException("Fine", "id", request.getFineId()));
        User librarian = userRepository.findById(librarianId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", librarianId));
        if (!(librarian.isAdmin() || librarian.isLibrarian())) {
            throw new ForbiddenException("Only librarians can process payments");
        }
        validatePaymentMethod(request.getPaymentMethod());
        fine.pay(request.getPaymentMethod(), librarian);
        fineRepository.save(fine);
        log.info("Fine {} paid by librarian {} for user {} via {}", fine.getFineId(), librarianId, fine.getUser().getUserId(), request.getPaymentMethod());
        return fineMapper.toFineResponse(fine);
    }

    @Override
    public FineResponse waiveFine(WaiveFineRequest request, Long adminId) {
        Fine fine = fineRepository.findById(request.getFineId())
                .orElseThrow(() -> new ResourceNotFoundException("Fine", "id", request.getFineId()));
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminId));
        if (!admin.isAdmin()) {
            throw new ForbiddenException("Only admins can waive fines");
        }
        fine.waive(request.getReason(), admin);
        fineRepository.save(fine);
        log.info("Fine {} waived by admin {} for user {}. Reason: {}", fine.getFineId(), adminId, fine.getUser().getUserId(), request.getReason());
        return fineMapper.toFineResponse(fine);
    }

    @Override
    public BigDecimal getTotalOutstandingFines(Long userId) {
        return fineRepository.getTotalOutstandingFinesByUser(userId);
    }

    @Override
    public boolean canUserBorrow(Long userId) {
        return getTotalOutstandingFines(userId).compareTo(maxOutstandingFines) < 0;
    }

    @Override
    public void calculateDailyFines() {
        List<Loan> overdueLoans = loanRepository.findOverdueLoans(LocalDateTime.now());
        int processed = 0;
        for (Loan loan : overdueLoans) {
            int daysOverdue = (int) loan.getDaysOverdue();
            if (daysOverdue <= 0) {
                continue;
            }
            Optional<Fine> existing = fineRepository.findByLoan_LoanId(loan.getLoanId());
            if (existing.isPresent()) {
                Fine fine = existing.get();
                if (fine.getStatus() == FineStatus.PENDING) {
                    fine.setDaysOverdue(daysOverdue);
                    fine.setAmount(Fine.calculateAmount(daysOverdue, fineRatePerDay));
                    fineRepository.save(fine);
                    processed++;
                }
            } else {
                createFine(loan.getLoanId(), daysOverdue);
                processed++;
            }
        }
        log.info("Daily fine calculation completed. Processed {} fines", processed);
    }

    private void validatePaymentMethod(String paymentMethod) {
        List<String> methods = Arrays.asList("CASH", "CARD", "BANK_TRANSFER", "ONLINE");
        if (!methods.contains(paymentMethod)) {
            throw new InvalidOperationException("Invalid payment method: " + paymentMethod);
        }
    }
}


