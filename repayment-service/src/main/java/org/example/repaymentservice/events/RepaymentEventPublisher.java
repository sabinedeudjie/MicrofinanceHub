package org.example.repaymentservice.events;

import org.example.repaymentservice.config.RabbitMQConfig;
import org.example.repaymentservice.model.Payment;
import org.example.repaymentservice.model.Schedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RepaymentEventPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    
    public void publishPaymentReceived(Payment payment, List<Schedule> paidSchedules, String clientEmail, String clientNom) {
        PaymentReceivedEvent event = PaymentReceivedEvent.builder()
            .paymentId(payment.getId())
            .paymentNumber(payment.getPaymentNumber())
            .loanId(payment.getLoanId())
            .clientId(payment.getClientId())
            .clientEmail(clientEmail)
            .clientNom(clientNom)
            .amount(payment.getAmount())
            .penaltyAmount(payment.getPenaltyAmount() != null ? payment.getPenaltyAmount() : BigDecimal.ZERO)
            .totalAmount(payment.getAmount().add(payment.getPenaltyAmount() != null ? payment.getPenaltyAmount() : BigDecimal.ZERO))
            .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : "UNKNOWN")
            .paymentDate(payment.getPaymentDate())
            .paidInstallments(paidSchedules.size())
            .timestamp(LocalDateTime.now())
            .build();
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.REPAYMENT_EXCHANGE, 
            RabbitMQConfig.PAYMENT_RECEIVED_KEY, 
            event
        );
        log.info("Événement PaymentReceived publié: paymentId={}, loanId={}, amount={}", 
            payment.getId(), payment.getLoanId(), payment.getAmount());
    }
    
    public void publishPaymentOverdue(Schedule schedule, BigDecimal penaltyAmount, int daysOverdue) {
        //  UUID en String avec .toString()
        PaymentOverdueEvent event = PaymentOverdueEvent.builder()
            .scheduleId(schedule.getId().toString())  // ←  UUID en String
            .loanId(schedule.getLoanId())
            .installmentNumber(schedule.getInstallmentNumber())
            .dueAmount(schedule.getDueAmount())
            .penaltyAmount(penaltyAmount)
            .daysOverdue(daysOverdue)
            .dueDate(schedule.getDueDate())
            .timestamp(LocalDateTime.now())
            .eventType("PAYMENT_OVERDUE")
            .build();
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.REPAYMENT_EXCHANGE, 
            RabbitMQConfig.PAYMENT_OVERDUE_KEY, 
            event
        );
        
        log.info("Événement PaymentOverdue publié: loanId={}, installment={}, daysOverdue={}", 
            schedule.getLoanId(), schedule.getInstallmentNumber(), daysOverdue);
    }
    
    public void publishScheduleUpdated(Schedule schedule) {
        //  UUID en String avec .toString()
        ScheduleUpdatedEvent event = ScheduleUpdatedEvent.builder()
            .scheduleId(schedule.getId().toString())  // ←  UUID en String
            .loanId(schedule.getLoanId())
            .installmentNumber(schedule.getInstallmentNumber())
            .paidAmount(schedule.getPaidAmount())
            .remainingAmount(schedule.getRemainingAmount())
            .status(schedule.isPaid() ? "PAID" : "PENDING")
            .paidDate(schedule.getPaidDate())
            .updatedDate(LocalDateTime.now())
            .eventType("SCHEDULE_UPDATED")
            .build();
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.REPAYMENT_EXCHANGE, 
            RabbitMQConfig.SCHEDULE_UPDATED_KEY, 
            event
        );
        
        log.debug("Événement ScheduleUpdated publié: loanId={}, installment={}, status={}", 
            schedule.getLoanId(), schedule.getInstallmentNumber(), schedule.isPaid() ? "PAID" : "PENDING");
    }
}