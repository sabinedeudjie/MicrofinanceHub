package org.example.transactionservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileMoneyRemboursementConfirmeEvent {
    private String loanId;
    private String clientId;
    private String clientEmail;
    private String clientNom;
    private BigDecimal montant;
    private String referenceRepayment;
    private String campayReference;
    private LocalDateTime timestamp;
}
