package org.example.loanservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EligibilityResponse {
    
    private boolean eligible;
    private BigDecimal maxEligibleAmount;
    private Integer maxTermMonths;
    private String message;
    private String clientId;
    private String clientName;
    private String clientEmail; 
    private String accountNumber;
    private BigDecimal accountBalance;
}