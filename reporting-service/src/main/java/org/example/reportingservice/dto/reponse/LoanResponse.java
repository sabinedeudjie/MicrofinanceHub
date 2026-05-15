package org.example.reportingservice.dto.reponse;

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
public class LoanResponse {
    private String id;
    private String loanNumber;
    private String clientId;
    private String clientEmail;
    private String clientFirstName;
    private String clientLastName;
    private BigDecimal amount;
    private BigDecimal interestRate;
    private Integer termMonths;
    private String repaymentFrequency;
    private BigDecimal monthlyPayment;
    private BigDecimal totalRepayment;
    private BigDecimal remainingBalance;
    private String status;
    private LocalDateTime disbursementDate;
    private LocalDateTime nextPaymentDate;
    private LocalDateTime maturityDate;
}