package org.example.reportingservice.dto.reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientDashboardResponse {
    
    //  des prêts
    private Integer totalLoans;
    private BigDecimal totalBorrowed;
    private BigDecimal totalRepaid;
    private BigDecimal remainingBalance;
    
    //  échéance
    private LocalDateTime nextPaymentDate;
    private BigDecimal nextPaymentAmount;
    
    //  de remboursement
    private Double repaymentRate;
    
    //  des prêts (optionnel)
    private List<LoanSummary> loans;
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LoanSummary {
        private String loanNumber;
        private BigDecimal amount;
        private BigDecimal remainingBalance;
        private String status;
        private LocalDateTime nextPaymentDate;
        private BigDecimal nextPaymentAmount;
    }
}


//  org.example.reportingservice.dto.reponse;

//  lombok.AllArgsConstructor;
//  lombok.Builder;
//  lombok.Data;
//  lombok.NoArgsConstructor;

//  java.math.BigDecimal;
//  java.time.LocalDateTime;

// 
// 
// 
// 
//  class ClientDashboardResponse {
//      Integer totalLoans;
//      BigDecimal totalBorrowed;
//      BigDecimal totalRepaid;
//      BigDecimal remainingBalance;
//      LocalDateTime nextPaymentDate;
//      BigDecimal nextPaymentAmount;
//      Double repaymentRate;
// 