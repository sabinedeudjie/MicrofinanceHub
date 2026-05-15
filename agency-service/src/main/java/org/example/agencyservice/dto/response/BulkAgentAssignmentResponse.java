package org.example.agencyservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkAgentAssignmentResponse {
    
    private String agencyId;
    private String agencyCode;
    private String agencyName;
    private int totalRequested;
    private int successCount;
    private int failedCount;
    private List<AssignmentResult> results;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentResult {
        private String agentId;
        private String agentEmail;
        private String agentName;
        private boolean success;
        private String reference;
        private String reason;
        private String errorMessage;
        private String assignmentId;
    }
}