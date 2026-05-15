package org.example.loanservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.example.loanservice.model.enums.ApplicationStatus;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoanApplicationResponse {
    
    private String id;
    private String applicationNumber;
    private String clientId;
    private String clientEmail;
    private String clientFirstName;
    private String clientLastName;
    private BigDecimal requestedAmount;
    private Integer termMonths;
    private String purpose;
    private ApplicationStatus status;
    private LocalDateTime applicationDate;
    private String rejectionReason;
}