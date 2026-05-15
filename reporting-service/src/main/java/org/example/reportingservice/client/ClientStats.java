package org.example.reportingservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientStats {
    private Long totalClients;
    private Long activeClients;
    private Long newClientsThisMonth;
    private Double clientGrowthRate;
    
    //  explicites pour être sûr
    public Long getTotalClients() { return totalClients; }
    public Long getActiveClients() { return activeClients; }
    public Long getNewClientsThisMonth() { return newClientsThisMonth; }
    public Double getClientGrowthRate() { return clientGrowthRate; }
}