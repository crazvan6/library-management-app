package com.library.management.service;

import com.library.management.dto.request.PayFineRequest;
import com.library.management.dto.request.WaiveFineRequest;
import com.library.management.dto.response.FineResponse;
import com.library.management.dto.response.UserFinesSummaryResponse;

import java.math.BigDecimal;
import java.util.List;

public interface FineService {
    FineResponse createFine(Long loanId, int daysOverdue);

    FineResponse getFineById(Long fineId);

    FineResponse getFineByLoanId(Long loanId);

    List<FineResponse> getMyFines(Long userId);

    List<FineResponse> getPendingFines(Long userId);

    UserFinesSummaryResponse getMyFinesSummary(Long userId);

    List<FineResponse> getAllPendingFines();

    FineResponse payFine(PayFineRequest request, Long librarianId);

    FineResponse waiveFine(WaiveFineRequest request, Long adminId);

    BigDecimal getTotalOutstandingFines(Long userId);

    boolean canUserBorrow(Long userId);

    void calculateDailyFines();
}


