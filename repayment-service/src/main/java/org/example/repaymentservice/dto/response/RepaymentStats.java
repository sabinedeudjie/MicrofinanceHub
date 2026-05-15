package org.example.repaymentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepaymentStats {
    private BigDecimal totalRepayments;
    private Long totalTransactions;
    private BigDecimal overdueAmount;
    private Long overdueCount;
    private Double repaymentRate;
}