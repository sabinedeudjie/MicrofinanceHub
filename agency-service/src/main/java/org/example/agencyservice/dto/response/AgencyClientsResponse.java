package org.example.agencyservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgencyClientsResponse {
    private String agencyId;
    private String agencyCode;
    private String agencyName;
    private Integer totalClients;
    private Integer totalAccounts;
    private BigDecimal totalBalance;
    private List<AgencyClientInfo> clients;
}