package org.example.repaymentservice.dto.response;

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
public class OverdueResponse {
    
    private String scheduleId;
    private String loanId;
    private Integer installmentNumber;
    private LocalDateTime dueDate;
    private BigDecimal dueAmount;
    private BigDecimal penaltyAmount;
    private Integer daysOverdue;
}