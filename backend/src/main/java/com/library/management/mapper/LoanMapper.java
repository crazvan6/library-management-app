package com.library.management.mapper;

import com.library.management.dto.request.CheckoutRequest;
import com.library.management.dto.response.LoanResponse;
import com.library.management.dto.response.LoanSummaryResponse;
import com.library.management.entity.Book;
import com.library.management.entity.Loan;
import com.library.management.entity.Reservation;
import com.library.management.entity.User;
import com.library.management.enums.LoanStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class LoanMapper {

    public LoanResponse toLoanResponse(Loan loan) {
        if (loan == null) {
            return null;
        }
        return LoanResponse.builder()
                .loanId(loan.getLoanId())
                .userId(loan.getUser().getUserId())
                .userFullName(loan.getUser().getFullName())
                .bookId(loan.getBook().getBookId())
                .bookTitle(loan.getBook().getTitle())
                .bookAuthor(loan.getBook().getAuthor())
                .bookIsbn(loan.getBook().getIsbn())
                .reservationId(loan.getReservation() != null ? loan.getReservation().getReservationId() : null)
                .checkoutDate(loan.getCheckoutDate())
                .dueDate(loan.getDueDate())
                .returnDate(loan.getReturnDate())
                .renewalCount(loan.getRenewalCount())
                .isRenewable(loan.getIsRenewable())
                .canBeRenewed(loan.canBeRenewed())
                .status(loan.getStatus())
                .isOverdue(loan.isOverdue())
                .daysUntilDue(loan.getDaysUntilDue())
                .daysOverdue(loan.getDaysOverdue())
                .checkedOutByName(loan.getCheckedOutBy() != null ? loan.getCheckedOutBy().getFullName() : null)
                .returnedByName(loan.getReturnedBy() != null ? loan.getReturnedBy().getFullName() : null)
                .hasFine(loan.getFine() != null)
                .createdAt(loan.getCreatedAt())
                .build();
    }

    public LoanSummaryResponse toLoanSummaryResponse(Loan loan) {
        return LoanSummaryResponse.builder()
                .loanId(loan.getLoanId())
                .bookTitle(loan.getBook().getTitle())
                .bookAuthor(loan.getBook().getAuthor())
                .checkoutDate(loan.getCheckoutDate())
                .dueDate(loan.getDueDate())
                .status(loan.getStatus())
                .isOverdue(loan.isOverdue())
                .daysUntilDue(loan.getDaysUntilDue())
                .canBeRenewed(loan.canBeRenewed())
                .build();
    }

    public Loan toEntity(CheckoutRequest request, User user, Book book, Reservation reservation, User librarian, int loanDurationDays, boolean renewable) {
        LocalDateTime now = LocalDateTime.now();
        return Loan.builder()
                .user(user)
                .book(book)
                .reservation(reservation)
                .checkedOutBy(librarian)
                .status(LoanStatus.ACTIVE)
                .checkoutDate(now)
                .dueDate(now.plusDays(loanDurationDays))
                .renewalCount(0)
                .isRenewable(renewable)
                .build();
    }
}


