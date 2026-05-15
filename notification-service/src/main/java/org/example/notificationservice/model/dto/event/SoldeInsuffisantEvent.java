package org.example.notificationservice.model.dto.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoldeInsuffisantEvent {
    private Long compteId;
    private Long clientId;
    private String clientNom;
    private String clientEmail;
    private String numeroCompte;
    private BigDecimal soldeActuel;
    private BigDecimal montantDemande;
    private LocalDateTime timestamp;
}
