package org.example.agencyservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {
    private String id;
    private String accountNumber;
    private String clientId;
    private String accountName;
    private String accountType;
    private BigDecimal balance;
    private String currency;
    private String status;
    private String description;
    private String agencyId;
    private String agencyCode;
    private String agencyName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}