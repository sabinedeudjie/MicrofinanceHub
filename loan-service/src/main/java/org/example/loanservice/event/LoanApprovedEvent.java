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
public class LoanApprovedEvent {
    
    private String loanId;
    private String loanNumber;
    private String clientId;
    private String clientEmail;
    private String clientFirstName;
    private String clientLastName;
    private BigDecimal amount;
    private BigDecimal monthlyPayment;
    private Integer termMonths;
    private BigDecimal interestRate;
    private LocalDateTime timestamp;
    @Builder.Default
    private String eventType = "LOAN_APPROVED";
}