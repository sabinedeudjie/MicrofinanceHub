package org.example.reportingservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortfolioStats {
    private BigDecimal totalOutstanding;
    private BigDecimal atRisk30Days;
    private BigDecimal atRisk90Days;
    private Double recoveryRate;
    
    //  explicites
    public BigDecimal getTotalOutstanding() { return totalOutstanding; }
    public BigDecimal getAtRisk30Days() { return atRisk30Days; }
    public BigDecimal getAtRisk90Days() { return atRisk90Days; }
    public Double getRecoveryRate() { return recoveryRate; }
}