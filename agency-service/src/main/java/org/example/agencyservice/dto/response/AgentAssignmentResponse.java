package org.example.agencyservice.dto.response;

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
    private String reason;
    private String reference;
    private LocalDateTime assignedAt;
    private boolean active;
}

//  org.example.agencyservice.dto.response;

//  lombok.AllArgsConstructor;
//  lombok.Builder;
//  lombok.Data;
//  lombok.NoArgsConstructor;
//  java.time.LocalDateTime;

// 
// 
// 
// 
//  class AgentAssignmentResponse {
//      String id;
//      String agentId;
//      String agentEmail;
//      String agentName;
//      String agencyId;
//      String agencyCode;
//      String agencyName;
//      String role;
//      String assignedBy;
//      String assignedByName;
//      LocalDateTime assignedAt;
//      boolean active;
// 
