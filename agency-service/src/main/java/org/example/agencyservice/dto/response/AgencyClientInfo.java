package org.example.agencyservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgencyClientInfo {
    private String clientId;
    private String clientEmail;
    private String clientFirstName;
    private String clientLastName;
    private String clientPhone;
    private LocalDateTime clientCreatedAt;
    private String clientStatus;
    private Integer clientCreditScore;
    private String clientCreatedBy;
    private List<AgencyAccountInfo> accounts;
    private Integer totalAccounts;
    private BigDecimal totalBalance;
}