package org.example.repaymentservice.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReceivedEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();
    
    private String paymentId;
    private String paymentNumber;
    private String loanId;
    private String clientId;
    private String clientEmail;
    private String clientNom;
    private BigDecimal amount;
    private BigDecimal penaltyAmount;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private LocalDateTime paymentDate;
    private Integer paidInstallments;
    private LocalDateTime timestamp;
    
    @Builder.Default
    private String eventType = "PAYMENT_RECEIVED";
}