package org.example.reportingservice.dto.reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardResponse {
    
    private LocalDateTime timestamp;
    
    //  clés
    private Long totalClients;
    private Long activeLoans;
    private BigDecimal totalOutstanding;
    private BigDecimal totalRepaymentsThisMonth;
    private Double repaymentRate;
    private Double defaultRate;
    
    // 
    private Double clientGrowthRate;
    private Double loanGrowthRate;
    
    // 
    private Long loansOverdue30Days;
    private Long loansOverdue90Days;
    private Long pendingApplications;
}