package org.example.transactionservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Représentation légère d'un compte reçu depuis account-service.
 */
@Data
@NoArgsConstructor
public class CompteInfo {
    private Long id;
    private Long clientId;
    private String numeroCompte;
    private BigDecimal solde;
    private BigDecimal plafond;
    private BigDecimal soldeMinimum;
    private String statut;     // "", "BLOQUE", "FERME", etc.
    private String typeCompte;
    private String clientEmail;
    private String clientNom;

    public boolean isOperationnel() {
        return "ACTIF".equals(statut);
    }

    public boolean hasSoldeSuffisant(BigDecimal montant) {
        BigDecimal minimum = soldeMinimum != null ? soldeMinimum : BigDecimal.ZERO;
        return solde != null && solde.subtract(montant).compareTo(minimum) >= 0;
    }

    public boolean respectePlafond(BigDecimal montantACrediter) {
        if (plafond == null || plafond.compareTo(BigDecimal.ZERO) == 0) return true;
        BigDecimal futurSolde = (solde != null ? solde : BigDecimal.ZERO).add(montantACrediter);
        return futurSolde.compareTo(plafond) <= 0;
    }
}
