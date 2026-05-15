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
public class LoanReportResponse {
    
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    
    private Long totalApplications;
    private Long approvedApplications;
    private Long rejectedApplications;
    private Long disbursedLoans;
    private Long activeLoans;
    private Long completedLoans;
    private Long defaultedLoans;
    
    private BigDecimal totalDisbursedAmount;
    private BigDecimal totalRepaidAmount;
    private BigDecimal outstandingAmount;
    private BigDecimal averageLoanSize;
    
    private Double approvalRate;
    private Double defaultRate;
    private Double recoveryRate;
    
    private List<LoanTrend> monthlyTrends;
    private List<ProductBreakdown> productBreakdown;
    
    @Data
    @Builder
    public static class LoanTrend {
        private String month;
        private Long applications;
        private BigDecimal amount;
    }
    
    @Data
    @Builder
    public static class ProductBreakdown {
        private String productName;
        private Long count;
        private BigDecimal amount;
        private Double percentage;
    }
}