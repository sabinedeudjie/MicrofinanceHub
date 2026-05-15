package org.example.accountservice.event;

import lombok.*;
import org.example.accountservice.model.EventType;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompteOuvertEvent {
    private EventType eventType = EventType.COMPTE_OUVERT;
    private String typeEvent = "OUVERT";
    private Long compteId;
    private String clientId;
    private String clientEmail;
    private String clientNom;
    private String numeroCompte;
    private String typeCompte;
    private LocalDateTime dateOuverture;
    private LocalDateTime timestamp;
}
