package org.example.clientservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientStatsResponse {
    private Long totalClients;
    private Long activeClients;
    private Long inactiveClients;
    private Long pendingClients;
    private Long suspendedClients;
    private Long newClientsThisMonth;
    private Long newClientsThisWeek;
    private Double clientGrowthRate;
    private Double averageCreditScore;
}


//  org.example.clientservice.dto.response;

//  lombok.AllArgsConstructor;
//  lombok.Builder;
//  lombok.Data;
//  lombok.NoArgsConstructor;

// 
// 
// 
// 
//  class ClientStatsResponse {
//      Long totalClients;
//      Long activeClients;
//      Long newClientsThisMonth;
//      Double clientGrowthRate;
// 