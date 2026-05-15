package org.example.transactionservice.dto;

import lombok.*;
import org.example.transactionservice.model.ModePaiement;
import org.example.transactionservice.model.StatutTransaction;
import org.example.transactionservice.model.TypeTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private Long id;
    private Long compteId;
    private TypeTransaction typeTransaction;
    private BigDecimal montant;
    private BigDecimal soldeAvant;
    private BigDecimal soldeApres;
    private LocalDateTime dateTransaction;
    private String reference;
    private StatutTransaction statut;
    private String description;
    private ModePaiement modePaiement;
    private String numeroPaiement;
    private String campayReference;
    private LocalDateTime createdAt;
}
