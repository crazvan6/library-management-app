package com.library.management.service;

import com.library.management.dto.request.CheckoutRequest;
import com.library.management.dto.request.ReturnBookRequest;
import com.library.management.dto.response.CheckoutResponse;
import com.library.management.dto.response.LoanResponse;
import com.library.management.dto.response.LoanSummaryResponse;
import com.library.management.dto.response.PageResponse;
import com.library.management.dto.response.ReturnBookResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LoanService {
    CheckoutResponse checkout(CheckoutRequest request, Long librarianId);

    LoanResponse getLoanById(Long loanId);

    List<LoanResponse> getMyLoans(Long userId);

    List<LoanResponse> getActiveLoans(Long userId);

    List<LoanResponse> getAllActiveLoans();

    List<LoanResponse> getOverdueLoans();

    PageResponse<LoanSummaryResponse> getLoansPage(Pageable pageable);

    LoanResponse getLoanByReservation(Long reservationId);

    void updateOverdueLoans();

    ReturnBookResponse returnBook(ReturnBookRequest request, Long librarianId);
}

