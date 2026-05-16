package org.example.transactionservice.event;

import lombok.*;
import org.example.transactionservice.model.EventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEffectueeEvent {
    private EventType eventType;
    private String typeEvent;
    private Long transactionId;
    private Long compteId;
    private Long clientId;
    private String clientEmail;
    private String clientNom;
    private String numeroCompte;
    /** Numéro du compte contrepartie (pour les virements) */
    private String compteContrepartie;
    /** Nom du titulaire de la contrepartie (expéditeur pour VIREMENT_ENTRANT) */
    private String nomContrepartie;
    private String typeCompte;
    private BigDecimal montant;
    private BigDecimal soldeApres;
    private String reference;
    private String description;
    private LocalDateTime dateTransaction;
    private LocalDateTime timestamp;
}
