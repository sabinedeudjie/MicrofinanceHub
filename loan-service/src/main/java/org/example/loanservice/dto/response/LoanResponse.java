package org.example.loanservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.example.loanservice.model.enums.LoanStatus;
import org.example.loanservice.model.enums.RepaymentFrequency;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
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
    private RepaymentFrequency repaymentFrequency;
    private BigDecimal monthlyPayment;
    private BigDecimal totalRepayment;
    private BigDecimal remainingBalance;
    private LoanStatus status;
    private LocalDateTime disbursementDate;
    private LocalDateTime nextPaymentDate;
    private LocalDateTime maturityDate;
    private List<AmortizationEntryResponse> amortizationSchedule;
}