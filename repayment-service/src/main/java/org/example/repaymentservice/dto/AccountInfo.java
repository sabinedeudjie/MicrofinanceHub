package org.example.repaymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountInfo {
    private String id;
    private String accountNumber;
    private String clientId;
    private String accountName;
    private String accountType;
    private BigDecimal balance;
    private String currency;
    private String status;
    private String agencyId;
    private String agencyCode;
    private String agencyName;
}