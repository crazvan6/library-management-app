package com.library.management.service;

import com.library.management.dto.request.PayFineRequest;
import com.library.management.dto.request.WaiveFineRequest;
import com.library.management.dto.response.FineResponse;
import com.library.management.dto.response.UserFinesSummaryResponse;
import com.library.management.entity.Fine;
import com.library.management.entity.Loan;
import com.library.management.entity.User;
import com.library.management.enums.FineStatus;
import com.library.management.enums.LoanStatus;
import com.library.management.enums.UserRole;
import com.library.management.exception.DuplicateResourceException;
import com.library.management.exception.ForbiddenException;
import com.library.management.exception.InvalidOperationException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.mapper.FineMapper;
import com.library.management.repository.FineRepository;
import com.library.management.repository.LoanRepository;
import com.library.management.repository.UserRepository;
import com.library.management.service.impl.FineServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FineServiceImplTest {

    @Mock private FineRepository fineRepository;
    @Mock private LoanRepository loanRepository;
    @Mock private UserRepository userRepository;
    @Mock private FineMapper fineMapper;

    private FineServiceImpl fineService;

    @BeforeEach
    void setup() {
        fineService = new FineServiceImpl(fineRepository, loanRepository, userRepository, fineMapper);
        ReflectionTestUtils.setField(fineService, "fineRatePerDay", new BigDecimal("0.50"));
        ReflectionTestUtils.setField(fineService, "maxOutstandingFines", new BigDecimal("10.00"));
    }

    // ---------- createFine ----------

    @Test
    void createFine_success_savesAndReturns() {
        User student = user(1L, UserRole.STUDENT);
        Loan loan = loan(10L, student);
        when(loanRepository.findById(10L)).thenReturn(Optional.of(loan));
        when(fineRepository.existsByLoan_LoanId(10L)).thenReturn(false);
        when(fineMapper.toEntity(any(Loan.class), any(User.class), anyInt(), any(BigDecimal.class)))
                .thenReturn(fine(5L, FineStatus.PENDING, student));

        fineService.createFine(10L, 3);

        verify(fineRepository).save(any(Fine.class));
    }

    @Test
    void createFine_loanNotFound_throws() {
        when(loanRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> fineService.createFine(10L, 3));
    }

    @Test
    void createFine_duplicateFine_throws() {
        when(loanRepository.findById(10L)).thenReturn(Optional.of(loan(10L, user(1L, UserRole.STUDENT))));
        when(fineRepository.existsByLoan_LoanId(10L)).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> fineService.createFine(10L, 3));
    }

    @Test
    void createFine_negativeDays_throws() {
        when(loanRepository.findById(10L)).thenReturn(Optional.of(loan(10L, user(1L, UserRole.STUDENT))));
        when(fineRepository.existsByLoan_LoanId(10L)).thenReturn(false);
        assertThrows(InvalidOperationException.class, () -> fineService.createFine(10L, -1));
    }

    // ---------- getFineById / getFineByLoanId ----------

    @Test
    void getFineById_found_returns() {
        when(fineRepository.findById(5L)).thenReturn(Optional.of(fine(5L, FineStatus.PENDING, user(1L, UserRole.STUDENT))));
        assertDoesNotThrow(() -> fineService.getFineById(5L));
    }

    @Test
    void getFineById_notFound_throws() {
        when(fineRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> fineService.getFineById(5L));
    }

    @Test
    void getFineByLoanId_found_returns() {
        when(fineRepository.findByLoan_LoanId(10L)).thenReturn(Optional.of(fine(5L, FineStatus.PENDING, user(1L, UserRole.STUDENT))));
        assertDoesNotThrow(() -> fineService.getFineByLoanId(10L));
    }

    @Test
    void getFineByLoanId_notFound_throws() {
        when(fineRepository.findByLoan_LoanId(10L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> fineService.getFineByLoanId(10L));
    }

    // ---------- getMyFines / getPendingFines / getAllPendingFines ----------

    @Test
    void getMyFines_userExists_returnsList() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(fineRepository.findByUser_UserId(1L)).thenReturn(List.of(fine(5L, FineStatus.PENDING, user(1L, UserRole.STUDENT))));
        List<FineResponse> result = fineService.getMyFines(1L);
        assertEquals(1, result.size());
    }

    @Test
    void getMyFines_userMissing_throws() {
        when(userRepository.existsById(1L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> fineService.getMyFines(1L));
    }

    @Test
    void getPendingFines_returnsList() {
        when(fineRepository.findPendingFinesByUser(1L)).thenReturn(List.of(fine(5L, FineStatus.PENDING, user(1L, UserRole.STUDENT))));
        assertEquals(1, fineService.getPendingFines(1L).size());
    }

    @Test
    void getAllPendingFines_returnsList() {
        when(fineRepository.findAllPendingFines()).thenReturn(List.of(fine(5L, FineStatus.PENDING, user(1L, UserRole.STUDENT))));
        assertEquals(1, fineService.getAllPendingFines().size());
    }

    // ---------- getMyFinesSummary ----------

    @Test
    void getMyFinesSummary_belowThreshold_canBorrow() {
        User student = user(1L, UserRole.STUDENT);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(fineRepository.getTotalOutstandingFinesByUser(1L)).thenReturn(new BigDecimal("5.00"));
        when(fineRepository.countByUserAndStatus(1L, FineStatus.PENDING)).thenReturn(2L);
        when(fineRepository.countByUserAndStatus(1L, FineStatus.PAID)).thenReturn(1L);
        when(fineRepository.countByUserAndStatus(1L, FineStatus.WAIVED)).thenReturn(0L);
        when(fineRepository.findByUser_UserId(1L)).thenReturn(List.of());

        UserFinesSummaryResponse summary = fineService.getMyFinesSummary(1L);
        assertTrue(summary.getCanBorrow());
        assertEquals(2, summary.getPendingFinesCount());
    }

    @Test
    void getMyFinesSummary_atThreshold_cannotBorrow() {
        User student = user(1L, UserRole.STUDENT);
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(fineRepository.getTotalOutstandingFinesByUser(1L)).thenReturn(new BigDecimal("10.00"));
        when(fineRepository.countByUserAndStatus(1L, FineStatus.PENDING)).thenReturn(1L);
        when(fineRepository.countByUserAndStatus(1L, FineStatus.PAID)).thenReturn(0L);
        when(fineRepository.countByUserAndStatus(1L, FineStatus.WAIVED)).thenReturn(0L);
        when(fineRepository.findByUser_UserId(1L)).thenReturn(List.of());

        assertFalse(fineService.getMyFinesSummary(1L).getCanBorrow());
    }

    @Test
    void getMyFinesSummary_userNotFound_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> fineService.getMyFinesSummary(1L));
    }

    // ---------- payFine ----------

    @Test
    void payFine_byLibrarian_marksPaid() {
        Fine fine = fine(5L, FineStatus.PENDING, user(1L, UserRole.STUDENT));
        when(fineRepository.findById(5L)).thenReturn(Optional.of(fine));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, UserRole.LIBRARIAN)));

        fineService.payFine(payRequest(5L, "CASH"), 2L);

        assertEquals(FineStatus.PAID, fine.getStatus());
        verify(fineRepository).save(fine);
    }

    @Test
    void payFine_fineNotFound_throws() {
        when(fineRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> fineService.payFine(payRequest(5L, "CASH"), 2L));
    }

    @Test
    void payFine_userNotFound_throws() {
        when(fineRepository.findById(5L)).thenReturn(Optional.of(fine(5L, FineStatus.PENDING, user(1L, UserRole.STUDENT))));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> fineService.payFine(payRequest(5L, "CASH"), 2L));
    }

    @Test
    void payFine_byStudent_forbidden() {
        when(fineRepository.findById(5L)).thenReturn(Optional.of(fine(5L, FineStatus.PENDING, user(1L, UserRole.STUDENT))));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, UserRole.STUDENT)));
        assertThrows(ForbiddenException.class, () -> fineService.payFine(payRequest(5L, "CASH"), 2L));
    }

    @Test
    void payFine_invalidPaymentMethod_throws() {
        when(fineRepository.findById(5L)).thenReturn(Optional.of(fine(5L, FineStatus.PENDING, user(1L, UserRole.STUDENT))));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, UserRole.LIBRARIAN)));
        assertThrows(InvalidOperationException.class, () -> fineService.payFine(payRequest(5L, "BITCOIN"), 2L));
    }

    // ---------- waiveFine ----------

    @Test
    void waiveFine_byAdmin_marksWaived() {
        Fine fine = fine(5L, FineStatus.PENDING, user(1L, UserRole.STUDENT));
        when(fineRepository.findById(5L)).thenReturn(Optional.of(fine));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user(3L, UserRole.ADMIN)));

        fineService.waiveFine(waiveRequest(5L, "Goodwill"), 3L);

        assertEquals(FineStatus.WAIVED, fine.getStatus());
        verify(fineRepository).save(fine);
    }

    @Test
    void waiveFine_byLibrarian_forbidden() {
        when(fineRepository.findById(5L)).thenReturn(Optional.of(fine(5L, FineStatus.PENDING, user(1L, UserRole.STUDENT))));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user(3L, UserRole.LIBRARIAN)));
        assertThrows(ForbiddenException.class, () -> fineService.waiveFine(waiveRequest(5L, "x"), 3L));
    }

    @Test
    void waiveFine_fineNotFound_throws() {
        when(fineRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> fineService.waiveFine(waiveRequest(5L, "x"), 3L));
    }

    // ---------- outstanding / canBorrow ----------

    @Test
    void getTotalOutstandingFines_delegatesToRepository() {
        when(fineRepository.getTotalOutstandingFinesByUser(1L)).thenReturn(new BigDecimal("4.00"));
        assertEquals(0, new BigDecimal("4.00").compareTo(fineService.getTotalOutstandingFines(1L)));
    }

    @Test
    void canUserBorrow_trueWhenBelowThreshold() {
        when(fineRepository.getTotalOutstandingFinesByUser(1L)).thenReturn(new BigDecimal("3.00"));
        assertTrue(fineService.canUserBorrow(1L));
    }

    @Test
    void canUserBorrow_falseWhenAtThreshold() {
        when(fineRepository.getTotalOutstandingFinesByUser(1L)).thenReturn(new BigDecimal("10.00"));
        assertFalse(fineService.canUserBorrow(1L));
    }

    // ---------- calculateDailyFines ----------

    @Test
    void calculateDailyFines_createsUpdatesAndSkips() {
        User student = user(1L, UserRole.STUDENT);
        Loan loanFuture = activeLoan(4L, student, LocalDateTime.now().plusDays(3));   // not overdue -> skipped
        Loan loanNew = activeLoan(2L, student, LocalDateTime.now().minusDays(5));     // overdue, no fine -> create
        Loan loanExisting = activeLoan(3L, student, LocalDateTime.now().minusDays(2)); // overdue, fine exists -> update

        when(loanRepository.findOverdueLoans(any(LocalDateTime.class)))
                .thenReturn(List.of(loanFuture, loanNew, loanExisting));
        when(fineRepository.findByLoan_LoanId(2L)).thenReturn(Optional.empty());
        when(loanRepository.findById(2L)).thenReturn(Optional.of(loanNew));
        when(fineRepository.existsByLoan_LoanId(2L)).thenReturn(false);
        when(fineMapper.toEntity(any(Loan.class), any(User.class), anyInt(), any(BigDecimal.class)))
                .thenReturn(fine(9L, FineStatus.PENDING, student));
        Fine existing = fine(8L, FineStatus.PENDING, student);
        when(fineRepository.findByLoan_LoanId(3L)).thenReturn(Optional.of(existing));

        fineService.calculateDailyFines();

        verify(fineRepository, times(2)).save(any(Fine.class)); // one new + one updated
        verify(fineRepository, never()).findByLoan_LoanId(4L);  // future loan skipped before lookup
    }

    // ---------- helpers ----------

    private User user(Long id, UserRole role) {
        return User.builder().userId(id).email("u" + id + "@test.com").firstName("F").lastName("L")
                .role(role).isActive(true).build();
    }

    private Loan loan(Long id, User user) {
        return Loan.builder().loanId(id).user(user).build();
    }

    private Loan activeLoan(Long id, User user, LocalDateTime dueDate) {
        return Loan.builder().loanId(id).user(user).status(LoanStatus.ACTIVE).dueDate(dueDate).build();
    }

    private Fine fine(Long id, FineStatus status, User user) {
        return Fine.builder().fineId(id).status(status).user(user)
                .amount(new BigDecimal("1.00")).daysOverdue(1).build();
    }

    private PayFineRequest payRequest(Long fineId, String method) {
        PayFineRequest r = new PayFineRequest();
        r.setFineId(fineId);
        r.setPaymentMethod(method);
        return r;
    }

    private WaiveFineRequest waiveRequest(Long fineId, String reason) {
        WaiveFineRequest r = new WaiveFineRequest();
        r.setFineId(fineId);
        r.setReason(reason);
        return r;
    }
}
