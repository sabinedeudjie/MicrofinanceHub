package org.example.loanservice.event;

import org.example.loanservice.config.RabbitMQConfig;
import org.example.loanservice.model.Loan;
import org.example.loanservice.model.LoanApplication;
import org.example.loanservice.model.Schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanEventPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    
    public void publishLoanApplied(LoanApplication application) {
        LoanAppliedEvent event = LoanAppliedEvent.builder()
            .applicationId(application.getId())
            .applicationNumber(application.getApplicationNumber())
            .clientId(application.getClientId())
            .clientEmail(application.getClientEmail())
            .clientFirstName(application.getClientFirstName())
            .clientLastName(application.getClientLastName())
            .amount(application.getRequestedAmount())
            .termMonths(application.getTermMonths())
            .purpose(application.getPurpose())
            .timestamp(application.getApplicationDate())
            .build();
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.LOAN_EXCHANGE, 
            RabbitMQConfig.LOAN_APPLIED_KEY, 
            event
        );
        log.info("LoanApplied publié: {}", event.getApplicationNumber());
    }
    
    public void publishLoanApproved(Loan loan) {
        LoanApprovedEvent event = LoanApprovedEvent.builder()
            .loanId(loan.getId())
            .loanNumber(loan.getLoanNumber())
            .clientId(loan.getClientId())
            .clientEmail(loan.getClientEmail())
            .clientFirstName(loan.getClientFirstName())
            .clientLastName(loan.getClientLastName())
            .amount(loan.getAmount())
            .monthlyPayment(loan.getMonthlyPayment())
            .termMonths(loan.getTermMonths())
            .interestRate(loan.getInterestRate())
            .timestamp(loan.getApprovalDate())
            .build();
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.LOAN_EXCHANGE, 
            RabbitMQConfig.LOAN_APPROVED_KEY, 
            event
        );
        log.info("LoanApproved publié: {}", loan.getLoanNumber());
    }
    
    public void publishLoanDisbursed(Loan loan) {
        LoanDisbursedEvent event = LoanDisbursedEvent.builder()
            .loanId(loan.getId())
            .loanNumber(loan.getLoanNumber())
            .clientId(loan.getClientId())
            .clientEmail(loan.getClientEmail())
            .clientFirstName(loan.getClientFirstName())
            .clientLastName(loan.getClientLastName())
            .amount(loan.getAmount())
            .disbursementDate(loan.getDisbursementDate())
            .nextPaymentDate(loan.getNextPaymentDate())
            .maturityDate(loan.getMaturityDate())
            .build();
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.LOAN_EXCHANGE, 
            RabbitMQConfig.LOAN_DISBURSED_KEY, 
            event
        );
        log.info("LoanDisbursed publié: {}", loan.getLoanNumber());
    }
    
    public void publishLoanRejected(LoanApplication application) {
        LoanRejectedEvent event = LoanRejectedEvent.builder()
            .applicationId(application.getId())
            .applicationNumber(application.getApplicationNumber())
            .clientId(application.getClientId())
            .clientEmail(application.getClientEmail())
            .clientFirstName(application.getClientFirstName())
            .clientLastName(application.getClientLastName())
            .reason(application.getRejectionReason())
            .timestamp(application.getReviewedDate())
            .build();
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.LOAN_EXCHANGE, 
            RabbitMQConfig.LOAN_REJECTED_KEY, 
            event
        );
        log.info("LoanRejected publié: {}", event.getApplicationNumber());
    }

    public void publishSchedulesGenerated(Loan loan, List<Schedule> schedules) {
        log.info("[EVENT] Schedules Generated: loanId={}, nbSchedules={}", 
            loan.getId(), schedules.size());
    }
}