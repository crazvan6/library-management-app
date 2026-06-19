package com.library.management.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasksService {

    private final ReservationService reservationService;
    private final LoanService loanService;
    private final FineService fineService;

    /**
     * Expire pending reservations older than hold window.
     * Runs hourly.
     */
    @Scheduled(cron = "${library.scheduling.expire-reservations-cron:0 0 * * * *}")
    public void expireReservationsJob() {
        log.info("Running scheduled job: expireReservations");
        reservationService.expireReservations();
    }

    /**
     * Mark overdue loans daily.
     */
    @Scheduled(cron = "${library.scheduling.update-overdue-loans-cron:0 0 1 * * *}")
    public void updateOverdueLoansJob() {
        log.info("Running scheduled job: updateOverdueLoans");
        loanService.updateOverdueLoans();
    }

    /**
     * Calculate/update fines daily at 1 AM.
     */
    @Scheduled(cron = "${library.scheduling.calculate-fines-cron:0 0 1 * * *}")
    public void calculateDailyFinesJob() {
        log.info("Running scheduled job: calculateDailyFines");
        try {
            fineService.calculateDailyFines();
            log.info("Completed scheduled job: calculateDailyFines");
        } catch (Exception ex) {
            log.error("Error during calculateDailyFines", ex);
        }
    }
}

