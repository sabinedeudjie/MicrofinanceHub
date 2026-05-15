package org.example.authservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private String agencyName;
    private String role;
    private String assignedBy;
    private String assignedByName;
    private LocalDateTime assignedAt;
    private boolean active;
}