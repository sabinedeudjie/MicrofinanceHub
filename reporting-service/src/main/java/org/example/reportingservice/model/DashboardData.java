package org.example.reportingservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardData {
    
    private LocalDateTime timestamp;
    private String period;
    
    // 
    private Long totalClients;
    private Long newClientsThisMonth;
    private Long activeClients;
    private Double clientGrowthRate;
    
    // 
    private Long totalLoanApplications;
    private Long approvedLoans;
    private Long activeLoans;
    private Long completedLoans;
    private Long defaultedLoans;
    private BigDecimal totalDisbursedAmount;
    private BigDecimal outstandingAmount;
    private Double approvalRate;
    private Double defaultRate;
    
    // 
    private BigDecimal totalRepayments;
    private BigDecimal overdueAmount;
    private Double repaymentRate;
    
    // 
    private Map<String, BigDecimal> portfolioComposition;
    private Map<String, Double> loanProductDistribution;
    
    // 
    private Double recoveryRate;
    private Double portfolioAtRisk30Days;
    private Double portfolioAtRisk90Days;
}