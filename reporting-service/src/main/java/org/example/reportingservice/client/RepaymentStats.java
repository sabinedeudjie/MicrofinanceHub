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
public class RepaymentStats {
    private BigDecimal totalRepayments;
    private Long totalTransactions;
    private BigDecimal overdueAmount;
    private Long overdueCount;
    private Double repaymentRate;
    
    //  explicites
    public BigDecimal getTotalRepayments() { return totalRepayments; }
    public Long getTotalTransactions() { return totalTransactions; }
    public BigDecimal getOverdueAmount() { return overdueAmount; }
    public Long getOverdueCount() { return overdueCount; }
    public Double getRepaymentRate() { return repaymentRate; }
}