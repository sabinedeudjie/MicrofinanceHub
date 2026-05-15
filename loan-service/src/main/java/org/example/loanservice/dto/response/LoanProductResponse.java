package org.example.loanservice.dto.response;

import org.example.loanservice.model.enums.RepaymentFrequency;
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
public class LoanProductResponse {
    private String id;
    private String code;
    private String name;
    private String description;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private Integer minTermMonths;
    private Integer maxTermMonths;
    private BigDecimal interestRate;
    private BigDecimal processingFeeRate;
    private BigDecimal insuranceRate;
    private RepaymentFrequency repaymentFrequency;
    private boolean active;
    private Integer minCreditScore;
    private BigDecimal maxDebtToIncomeRatio;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}