package org.example.agencyservice.dto.request;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgencyStatsUpdateRequest {
    
    private Long newClients;      //  de nouveaux clients
    private Long newAccounts;     //  de nouveaux comptes
    private Long newLoans;        //  de nouveaux prêts (optionnel)
    private Long newRepayments;   //  remboursements (optionnel)

   // absolu (ajouter ces champs)
    private Long totalClients;
    private Long totalAccounts;
    private Long totalLoans; 
}