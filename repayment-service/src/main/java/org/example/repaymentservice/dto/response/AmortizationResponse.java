package org.example.repaymentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmortizationResponse {
    
    private String loanId;
    private String loanNumber;
    private BigDecimal totalAmount;
    private BigDecimal totalInterest;
    private BigDecimal totalRepayment;
    private List<AmortizationEntry> entries;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AmortizationEntry {
        private Integer installmentNumber;
        private LocalDateTime dueDate;
        private BigDecimal dueAmount;
        private BigDecimal principalAmount;
        private BigDecimal interestAmount;
        private BigDecimal remainingBalance;
        private boolean paid;
    }
}