package org.example.accountservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "compte")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "numero_compte", nullable = false, unique = true, length = 20)
    private String numeroCompte;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "type_compte", nullable = false)
    private TypeCompte typeCompte;

    @Column(name = "solde", nullable = false, precision = 15, scale = 2)
    private BigDecimal solde;

    @Enumerated(EnumType.STRING)
    @Column(name = "devise", nullable = false, length = 10)
    private Devise devise;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "statut", nullable = false)
    private StatutCompte statut;

    @Column(name = "date_ouverture")
    private LocalDateTime dateOuverture;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    /** Taux d'intérêt annuel. Ex: 0.0350 = 3.50% */
    @Column(name = "taux_interet", precision = 5, scale = 4)
    private BigDecimal tauxInteret;

    @Column(name = "solde_minimum", precision = 15, scale = 2)
    private BigDecimal soldeMinimum;

    @Column(name = "plafond", precision = 15, scale = 2)
    private BigDecimal plafond;

    @Column(name = "description")
    private String description;

    @Column(name = "client_email")
    private String clientEmail;

    @Column(name = "client_nom")
    private String clientNom;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    /** Vérifie si le compte peut effectuer des opérations */
    public boolean isOperationnel() {
        return this.statut == StatutCompte.ACTIF;
    }

    /** Vérifie si le solde est suffisant pour un retrait */
    public boolean hasSoldeSuffisant(BigDecimal montant) {
        BigDecimal soldeApresRetrait = this.solde.subtract(montant);
        BigDecimal minimum = this.soldeMinimum != null ? this.soldeMinimum : BigDecimal.ZERO;
        return soldeApresRetrait.compareTo(minimum) >= 0;
    }

    /** Vérifie si un dépôt ne dépasse pas le plafond */
    public boolean respectePlafond(BigDecimal montant) {
        if (this.plafond == null) return true;
        return this.solde.add(montant).compareTo(this.plafond) <= 0;
    }

    /** Crédite le compte */
    public void crediter(BigDecimal montant) {
        this.solde = this.solde.add(montant);
        this.dateModification = LocalDateTime.now();
    }

    /** Débite le compte */
    public void debiter(BigDecimal montant) {
        this.solde = this.solde.subtract(montant);
        this.dateModification = LocalDateTime.now();
    }
}
