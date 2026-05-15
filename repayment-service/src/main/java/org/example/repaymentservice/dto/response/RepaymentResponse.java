package org.example.repaymentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RepaymentResponse {
    
    private String loanId;
    private String clientId;
    private String clientName;
    private BigDecimal totalPaid;
    private BigDecimal totalPenalty;
    private Integer paidInstallments;
    private Integer totalInstallments;
    private BigDecimal remainingBalance;
    private List<ScheduleResponse> schedule;
}