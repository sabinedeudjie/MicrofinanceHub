package org.example.repaymentservice.events;

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
public class LoanDisbursedEvent {
    
    private String loanId;
    private String loanNumber;
    private String clientId;
    private String clientEmail;
    private String clientFirstName;
    private String clientLastName;
    private BigDecimal amount;
    private BigDecimal interestRate;
    private Integer termMonths;
    private BigDecimal monthlyPayment;
    private LocalDateTime disbursementDate;
    private LocalDateTime nextPaymentDate;
    private LocalDateTime maturityDate;
    @Builder.Default
    private String eventType = "LOAN_DISBURSED";
}