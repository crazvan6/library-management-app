package com.library.management.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScheduledTasksServiceTest {

    @Mock
    private ReservationService reservationService;

    @Mock
    private LoanService loanService;

    @Mock
    private FineService fineService;

    @InjectMocks
    private ScheduledTasksService scheduledTasksService;

    @Test
    void expireReservationsJob_shouldDelegate() {
        scheduledTasksService.expireReservationsJob();
        verify(reservationService, times(1)).expireReservations();
    }

    @Test
    void updateOverdueLoansJob_shouldDelegate() {
        scheduledTasksService.updateOverdueLoansJob();
        verify(loanService, times(1)).updateOverdueLoans();
    }

    @Test
    void calculateDailyFinesJob_shouldDelegate() {
        scheduledTasksService.calculateDailyFinesJob();
        verify(fineService, times(1)).calculateDailyFines();
    }

    @Test
    void calculateDailyFinesJob_shouldSwallowExceptions() {
        doThrow(new RuntimeException("fail")).when(fineService).calculateDailyFines();

        assertDoesNotThrow(() -> scheduledTasksService.calculateDailyFinesJob());
        verify(fineService, times(1)).calculateDailyFines();
    }
}


