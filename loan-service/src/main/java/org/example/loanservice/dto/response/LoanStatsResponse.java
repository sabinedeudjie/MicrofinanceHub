package org.example.loanservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanStatsResponse {
    private Long totalApplications;
    private Long pendingApplications;
    private Long approvedApplications;
    private Long rejectedApplications;
    private Long disbursedLoans;
    private Long activeLoans;
    private Long completedLoans;
    private Long defaultedLoans;
    private BigDecimal totalDisbursedAmount;
    private BigDecimal totalRepaidAmount;
    private BigDecimal outstandingAmount;
    private Double approvalRate;
    private Double defaultRate;
    private Double recoveryRate;
}