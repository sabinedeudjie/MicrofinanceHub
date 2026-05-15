package org.example.agencyservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountStatsResponse {
    private String agencyId;
    private String agencyCode;
    private String agencyName;
    private Long totalClients;
    private Long totalAccounts;
    private Long totalBalance;
}