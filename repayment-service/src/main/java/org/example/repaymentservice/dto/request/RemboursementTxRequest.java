package org.example.repaymentservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemboursementTxRequest {
    private String loanId;
    private String clientId;
    private BigDecimal montant;
    private String modePaiement;
    private String numeroPaiement;
    private Long compteSourceId;
    private String description;
    private String referenceRepayment;
}
