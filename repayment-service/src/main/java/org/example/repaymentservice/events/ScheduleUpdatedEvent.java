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
public class ScheduleUpdatedEvent {
    
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();
    
    private String scheduleId;
    private String loanId;
    private String clientId;
    private Integer installmentNumber;
    private BigDecimal dueAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private String status;
    private LocalDateTime paidDate;
    private LocalDateTime updatedDate;
    private LocalDateTime timestamp;
    
    @Builder.Default
    private String eventType = "SCHEDULE_UPDATED";
}