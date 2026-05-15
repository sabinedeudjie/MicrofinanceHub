package org.example.loanservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanAppliedEvent {
    
    private String applicationId;
    private String applicationNumber;
    private String clientId;
    private String clientEmail;
    private String clientFirstName;
    private String clientLastName;
    private BigDecimal amount;
    private Integer termMonths;
    private String purpose;
    private LocalDateTime timestamp;
    @Builder.Default
    private String eventType = "LOAN_APPLIED";
}