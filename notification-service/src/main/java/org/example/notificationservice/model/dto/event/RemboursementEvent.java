package org.example.notificationservice.model.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

//  avec PaymentReceivedEvent et PaymentOverdueEvent de repayment-service
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class RemboursementEvent {
    private String    paymentId;
    private String    paymentNumber;
    private String    loanId;
    private String    scheduleId;
    private String    clientId;
    private String    clientEmail;
    private String    clientNom;
    private BigDecimal amount;
    private BigDecimal penaltyAmount;
    private BigDecimal totalAmount;
    private BigDecimal dueAmount;
    private String    paymentMethod;
    private Integer   daysOverdue;
    private Integer   installmentNumber;
    private LocalDateTime paymentDate;
    private LocalDateTime dueDate;
    private LocalDateTime timestamp;
    private String    eventType;   // "", "SCHEDULE_UPDATED"
}
