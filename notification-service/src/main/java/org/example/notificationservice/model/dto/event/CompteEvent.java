package org.example.notificationservice.model.dto.event;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompteEvent {
    private String    compteId;
    private String    clientId;
    private String    clientNom;
    private String    clientEmail;
    private String    clientTelephone;
    private String    numeroCompte;
    private String    typeCompte;      // "EPARGNE", "COURANT", etc.
    private java.math.BigDecimal montant;
    private java.math.BigDecimal soldeApres;
    private String    typeEvent;       // "OUVERT", "DEPOT", "RETRAIT", "VIREMENT_SORTANT", "VIREMENT_ENTRANT"
    private String    compteContrepartie;
    /** Nom du titulaire de la contrepartie (expéditeur pour VIREMENT_ENTRANT) */
    private String    nomContrepartie;
    private LocalDateTime dateEvenement;
}