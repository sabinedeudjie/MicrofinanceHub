package org.example.repaymentservice.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionTxResponse {
    private Long id;
    private String reference;
    private String campayReference;
    private String statut;
    private BigDecimal montant;
    private LocalDateTime dateTransaction;
}
