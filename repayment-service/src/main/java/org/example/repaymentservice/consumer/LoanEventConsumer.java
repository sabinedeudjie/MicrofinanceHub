package org.example.repaymentservice.consumer;

import org.example.repaymentservice.config.RabbitMQConfig;
import org.example.repaymentservice.events.LoanApprovedEvent;
import org.example.repaymentservice.events.LoanDisbursedEvent;
import org.example.repaymentservice.model.Schedule;
import org.example.repaymentservice.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanEventConsumer {
    
    private final ScheduleRepository scheduleRepository;
    
    @RabbitListener(queues = RabbitMQConfig.LOAN_DISBURSED_QUEUE)
    @Transactional
    public void handleLoanDisbursed(LoanDisbursedEvent event) {
        log.info("Réception de LoanDisbursedEvent pour loanId: {}", event.getLoanId());
        
        // Vérifier si les schedules existent déjà pour éviter les doublons ou erreurs de verrouillage
        if (scheduleRepository.countByLoanId(event.getLoanId()) > 0) {
            log.warn("Les échéances existent déjà pour le prêt {}. Ignoré.", event.getLoanId());
            return;
        }
        
        // Générer les schedules pour le Repayment Service
        List<Schedule> schedules = generateSchedulesFromLoan(event);
        scheduleRepository.saveAll(schedules);
        
        log.info(" {} schedules créés pour loanId: {}", schedules.size(), event.getLoanId());
    }
    
    @RabbitListener(queues = RabbitMQConfig.LOAN_APPROVED_QUEUE)
    @Transactional
    public void handleLoanApproved(LoanApprovedEvent event) {
        log.info("Réception de LoanApprovedEvent pour loanId: {}", event.getLoanId());
        
        log.info("Prêt approuvé : loanId={}, montant={}, mensualité={}", 
            event.getLoanId(), event.getAmount(), event.getMonthlyPayment());
    }
    
    private List<Schedule> generateSchedulesFromLoan(LoanDisbursedEvent event) {
        List<Schedule> schedules = new ArrayList<>();
        
        // Récupérer le taux d'intérêt depuis l'événement
        BigDecimal annualRate = event.getInterestRate() != null ? 
            event.getInterestRate() : new BigDecimal("12.5");
        
        int termMonths = event.getTermMonths() != null ? event.getTermMonths() : 12;
        
        // Calculer la mensualité
        BigDecimal monthlyPayment = calculateMonthlyPayment(event.getAmount(), annualRate, termMonths);
        
        LocalDateTime dueDate = event.getDisbursementDate().plusMonths(1);
        BigDecimal remainingBalance = event.getAmount();
        BigDecimal monthlyRate = annualRate
            .divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)
            .divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);
        
        for (int i = 1; i <= termMonths; i++) {
            BigDecimal interestAmount = remainingBalance.multiply(monthlyRate);
            BigDecimal principalAmount = monthlyPayment.subtract(interestAmount);
            
            if (i == termMonths) {
                principalAmount = remainingBalance;
            }
            
            remainingBalance = remainingBalance.subtract(principalAmount);
            if (remainingBalance.compareTo(BigDecimal.ZERO) < 0) {
                remainingBalance = BigDecimal.ZERO;
            }
            
            // LAISSER HIBERNATE GÉRER L'ID (GeneratedValue)
            Schedule schedule = Schedule.builder()
                .loanId(event.getLoanId())
                .installmentNumber(i)
                .dueDate(dueDate)
                .dueAmount(monthlyPayment)
                .principalAmount(principalAmount)
                .interestAmount(interestAmount)
                .remainingBalance(remainingBalance)
                .paid(false)
                .build();
            
            schedules.add(schedule);
            dueDate = dueDate.plusMonths(1);
        }
        
        return schedules;
    }
    
    private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualRate, int months) {
        if (months <= 0) return BigDecimal.ZERO;
        
        BigDecimal monthlyRate = annualRate
            .divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)
            .divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);
        
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }
        
        BigDecimal factor = BigDecimal.ONE.add(monthlyRate).pow(months);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(factor);
        BigDecimal denominator = factor.subtract(BigDecimal.ONE);
        
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }
}