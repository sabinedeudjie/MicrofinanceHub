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
public class PaymentOverdueEvent {
    
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();
    
    private String scheduleId;
    private String loanId;
    private String clientId;
    private Integer installmentNumber;
    private BigDecimal dueAmount;
    private BigDecimal penaltyAmount;
    private Integer daysOverdue;
    private LocalDateTime dueDate;
    private LocalDateTime timestamp;
    
    @Builder.Default
    private String eventType = "PAYMENT_OVERDUE";
}