package org.example.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.accountservice.model.Devise;
import org.example.accountservice.model.StatutCompte;
import org.example.accountservice.model.TypeCompte;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour un compte.
 * Renvoyé par l'API sans exposer les données internes JPA.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompteResponse {
    private Long id;
    private String clientId;
    private String numeroCompte;
    private TypeCompte typeCompte;
    private BigDecimal solde;
    private Devise devise;
    private StatutCompte statut;
    private LocalDateTime dateOuverture;
    private BigDecimal tauxInteret;
    private BigDecimal soldeMinimum;
    private BigDecimal plafond;
    private String description;
    private LocalDateTime createdAt;
    private String clientEmail;
    private String clientNom;
}
