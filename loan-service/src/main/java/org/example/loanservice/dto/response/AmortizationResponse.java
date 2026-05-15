package org.example.loanservice.dto.response;

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
public class AmortizationResponse {
    
    private String loanId;
    private String loanNumber;
    private BigDecimal totalAmount;
    private BigDecimal totalInterest;
    private BigDecimal totalRepayment;
    private List<AmortizationEntryResponse> entries;
}
