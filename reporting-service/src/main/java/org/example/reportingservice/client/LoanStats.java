package org.example.reportingservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoanStats {
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
    private Double approvalRate;
    private Double defaultRate;
    private Double recoveryRate;
    
    //  explicites
    public Long getTotalApplications() { return totalApplications; }
    public Long getApprovedApplications() { return approvedApplications; }
    public Long getRejectedApplications() { return rejectedApplications; }
    public Long getDisbursedLoans() { return disbursedLoans; }
    public Long getActiveLoans() { return activeLoans; }
    public Long getCompletedLoans() { return completedLoans; }
    public Long getDefaultedLoans() { return defaultedLoans; }
    public BigDecimal getTotalDisbursedAmount() { return totalDisbursedAmount; }
    public BigDecimal getTotalRepaidAmount() { return totalRepaidAmount; }
    public BigDecimal getOutstandingAmount() { return outstandingAmount; }
    public Double getApprovalRate() { return approvalRate; }
    public Double getDefaultRate() { return defaultRate; }
    public Double getRecoveryRate() { return recoveryRate; }
}