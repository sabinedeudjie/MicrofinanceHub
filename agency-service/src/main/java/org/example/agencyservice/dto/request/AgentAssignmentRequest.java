package org.example.agencyservice.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentAssignmentRequest {
    
    @NotBlank(message = "L'ID de l'agent est requis")
    private String agentId;
    
    private String agencyId;
    
    private String reason;  // 
    
    private String assignmentDate;  //  d'assignation (optionnel, si null = date courante)
    
    private String reference;  //  unique de l'assignation (optionnel)
}


//  org.example.agencyservice.dto.request;

//  jakarta.validation.constraints.NotBlank;
//  lombok.Data;

// 
//  class AgentAssignmentRequest {
    
//     (message = "L'ID de l'agent est requis")
//      String agentId;
    
//     (message = "L'email de l'agent est requis")
//      String agentEmail;
    
//     (message = "Le nom de l'agent est requis")
//      String agentName;
    
//     (message = "L'ID de l'agence est requis")
//      String agencyId;
    
//      String role;
// 