package com.library.management.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "library")
@Validated
@Data
public class AppProperties {
    private Fine fineConfig;
    private Loan loanConfig;
    private Reservation reservationConfig;

    @Data
    public static class Fine {
        @Min(0)
        private BigDecimal ratePerDay;
        @Min(0)
        private BigDecimal maxOutstanding;
    }

    @Data
    public static class Loan {
        @Min(1)
        private int durationDays;
        @Min(0)
        private int maxRenewals;
    }

    @Data
    public static class Reservation {
        @Min(1)
        private int holdHours;
    }
}


