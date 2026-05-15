package org.example.clientservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreditScoreResponse {
    
    private String clientId;
    private String clientName;
    private Integer creditScore;
    private String rating;
    private String recommendation;
}