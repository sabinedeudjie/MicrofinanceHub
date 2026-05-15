// .java
package org.example.repaymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentAssignmentResponse {
    private String id;
    private String agentId;
    private String agentEmail;
    private String agentName;
    private String agencyId;
    private String agencyCode;
    private boolean active;
}