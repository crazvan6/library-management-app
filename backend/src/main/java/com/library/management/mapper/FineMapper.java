package com.library.management.mapper;

import com.library.management.dto.response.FineResponse;
import com.library.management.dto.response.FineSummaryResponse;
import com.library.management.entity.Fine;
import com.library.management.entity.Loan;
import com.library.management.entity.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FineMapper {

    public FineResponse toFineResponse(Fine fine) {
        if (fine == null) return null;
        Loan loan = fine.getLoan();
        return FineResponse.builder()
                .fineId(fine.getFineId())
                .loanId(loan != null ? loan.getLoanId() : null)
                .userId(fine.getUser() != null ? fine.getUser().getUserId() : null)
                .userFullName(fine.getUser() != null ? fine.getUser().getFullName() : null)
                .bookTitle(loan != null ? loan.getBook().getTitle() : null)
                .bookAuthor(loan != null ? loan.getBook().getAuthor() : null)
                .amount(fine.getAmount())
                .daysOverdue(fine.getDaysOverdue())
                .status(fine.getStatus())
                .paymentDate(fine.getPaymentDate())
                .paymentMethod(fine.getPaymentMethod())
                .processedByName(fine.getProcessedBy() != null ? fine.getProcessedBy().getFullName() : null)
                .notes(fine.getNotes())
                .createdAt(fine.getCreatedAt())
                .updatedAt(fine.getUpdatedAt())
                .build();
    }

    public FineSummaryResponse toFineSummaryResponse(Fine fine) {
        Loan loan = fine.getLoan();
        return FineSummaryResponse.builder()
                .fineId(fine.getFineId())
                .bookTitle(loan != null ? loan.getBook().getTitle() : null)
                .amount(fine.getAmount())
                .daysOverdue(fine.getDaysOverdue())
                .status(fine.getStatus())
                .createdAt(fine.getCreatedAt())
                .build();
    }

    public Fine toEntity(Loan loan, User user, int daysOverdue, BigDecimal amount) {
        return Fine.builder()
                .loan(loan)
                .user(user)
                .daysOverdue(daysOverdue)
                .amount(amount)
                .status(com.library.management.enums.FineStatus.PENDING)
                .build();
    }
}


