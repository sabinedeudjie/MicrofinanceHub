package org.example.agencyservice.dto.request;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkAgentAssignmentRequest {
    
    @NotBlank(message = "L'ID de l'agence est requis")
    private String agencyId;
    
    @NotEmpty(message = "La liste des agents ne peut pas être vide")
    @Valid
    private List<AgentAssignmentItem> agents;
    
    private String globalReason;  //  globale pour tous les agents
    
    private String assignmentDate;  //  d'assignation commune
    
    private String globalReference;  //  globale (optionnel)
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentAssignmentItem {
        
        @NotBlank(message = "L'ID de l'agent est requis")
        private String agentId;
        
        private String reason;  //  spécifique à cet agent (surcharge la raison globale)
        
        private String reference;  //  spécifique à cet agent
    }
}