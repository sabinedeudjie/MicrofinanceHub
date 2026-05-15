package org.example.agencyservice.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgencyStatsResponse {
    private String agencyId;
    private String agencyCode;
    private String agencyName;
    private Long totalAgents;
    private Long activeAgents;
    private Long totalClients;
    private Long totalAccounts;
    private Long totalLoans;
    private BigDecimal totalOutstanding;
    private Double monthlyRepaymentRate;
}