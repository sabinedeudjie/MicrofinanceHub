package org.example.loanservice.service;

import org.example.loanservice.event.LoanDisbursedEvent;
import org.example.loanservice.model.Loan;
import org.example.loanservice.model.AmortizationSchedule;
import org.example.loanservice.repository.LoanRepository;
import org.example.loanservice.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.event.EventListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleManagementService {
    
    private final ScheduleRepository scheduleRepository;
    private final LoanRepository loanRepository;
    
    @EventListener
    @Transactional
    public void onLoanDisbursed(LoanDisbursedEvent event) {
        log.info("LoanDisbursed reçu pour loanId: {}", event.getLoanId());
        generateSchedules(event.getLoanId());
    }
    
    @Transactional
    public void ensureSchedulesExist(String loanId) {
        if (scheduleRepository.countByLoanId(loanId) == 0) {
            log.warn("schedule trouvé pour loanId: {}, régénération...", loanId);
            generateSchedules(loanId);
        }
    }
    
    @Transactional
    public List<AmortizationSchedule> generateSchedules(String loanId) {
        log.info("des schedules pour le prêt: {}", loanId);
        
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Prêt non trouvé: " + loanId));
        
        //  les anciens schedules
        scheduleRepository.deleteByLoanId(loanId);
        
        List<AmortizationSchedule> schedules = calculateAmortization(loan);
        schedules = scheduleRepository.saveAll(schedules);
        
        log.info("schedules générés pour le prêt: {}", schedules.size(), loanId);
        return schedules;
    }
    
    private List<AmortizationSchedule> calculateAmortization(Loan loan) {
        List<AmortizationSchedule> schedules = new ArrayList<>();
        
        BigDecimal remainingBalance = loan.getAmount();
        BigDecimal monthlyRate = loan.getInterestRate()
            .divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)
            .divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);
        
        LocalDateTime dueDate = loan.getDisbursementDate() != null ? 
            loan.getDisbursementDate() : LocalDateTime.now();
        
        for (int i = 1; i <= loan.getTermMonths(); i++) {
            dueDate = dueDate.plusMonths(1);
            
            BigDecimal interestAmount = remainingBalance.multiply(monthlyRate);
            BigDecimal principalAmount = loan.getMonthlyPayment().subtract(interestAmount);
            
            if (i == loan.getTermMonths()) {
                principalAmount = remainingBalance;
            }
            
            remainingBalance = remainingBalance.subtract(principalAmount);
            if (remainingBalance.compareTo(BigDecimal.ZERO) < 0) {
                remainingBalance = BigDecimal.ZERO;
            }
            
            AmortizationSchedule schedule = AmortizationSchedule.builder()
                .loan(loan)
                .installmentNumber(i)
                .dueDate(dueDate)
                .dueAmount(loan.getMonthlyPayment())
                .principalAmount(principalAmount)
                .interestAmount(interestAmount)
                .remainingBalance(remainingBalance)
                .paid(false)
                .build();
            
            schedules.add(schedule);
        }
        
        return schedules;
    }

    public List<AmortizationSchedule> getSchedulesByLoanId(String loanId) {
        log.info("des schedules pour le prêt: {}", loanId);
        return scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);
    }
    
    @Transactional
    public void markScheduleAsPaid(String loanId, Integer installmentNumber, String paymentId) {
        log.info("Mise à jour du schedule comme payé: loanId={}, installment={}, paymentId={}", 
            loanId, installmentNumber, paymentId);
        
        AmortizationSchedule schedule = scheduleRepository
            .findByLoanIdAndInstallmentNumber(loanId, installmentNumber)
            .orElseThrow(() -> new RuntimeException("Schedule non trouvé"));
        
        if (schedule.isPaid()) {
            log.warn("Le schedule est déjà marqué comme payé");
            return;
        }

        schedule.setPaid(true);
        schedule.setPaidDate(LocalDateTime.now());
        schedule.setPaymentId(paymentId);
        scheduleRepository.save(schedule);
        
        // Mettre à jour le prêt associé (solde et prochaine échéance)
        Loan loan = schedule.getLoan();
        if (loan != null) {
            // Déduire du solde restant
            BigDecimal currentBalance = loan.getRemainingBalance() != null ? 
                loan.getRemainingBalance() : loan.getTotalRepayment();
            
            BigDecimal newBalance = currentBalance.subtract(schedule.getDueAmount());
            if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
                newBalance = BigDecimal.ZERO;
                loan.setStatus(org.example.loanservice.model.enums.LoanStatus.COMPLETED);
                log.info("Félicitations ! Le prêt {} est maintenant ENTIÈREMENT REMBOURSÉ (Statut: COMPLETED)", loanId);
            }
            loan.setRemainingBalance(newBalance);
            
            // Trouver la prochaine échéance
            scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId)
                .stream()
                .filter(s -> !s.isPaid())
                .findFirst()
                .ifPresent(next -> loan.setNextPaymentDate(next.getDueDate()));
            
            loanRepository.save(loan);
            log.info("Prêt mis à jour: nouveau solde={}, prochaine échéance={}", 
                newBalance, loan.getNextPaymentDate());
        }
        
        log.info("marqué comme payé avec succès");
    }
}