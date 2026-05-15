package org.example.transactionservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "compte_id", nullable = false)
    private Long compteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_transaction", nullable = false)
    private TypeTransaction typeTransaction;

    @Column(name = "montant", nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    @Column(name = "solde_avant", nullable = false, precision = 15, scale = 2)
    private BigDecimal soldeAvant;

    @Column(name = "solde_apres", nullable = false, precision = 15, scale = 2)
    private BigDecimal soldeApres;

    @Column(name = "date_transaction")
    private LocalDateTime dateTransaction;

    @Column(name = "reference", nullable = false, unique = true, length = 50)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutTransaction statut;

    @Column(name = "description")
    private String description;

    @Column(name = "agent_id")
    private Long agentId;

    @Column(name = "compte_destination", length = 20)
    private String compteDestination;

    @Column(name = "loan_id", length = 100)
    private String loanId;

    @Column(name = "client_id", length = 100)
    private String clientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", length = 50)
    private ModePaiement modePaiement;

    @Column(name = "piece_justificative")
    private String pieceJustificative;

    @Column(name = "campay_reference", length = 100, unique = true)
    private String campayReference;

    @Column(name = "numero_paiement", length = 20)
    private String numeroPaiement;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
