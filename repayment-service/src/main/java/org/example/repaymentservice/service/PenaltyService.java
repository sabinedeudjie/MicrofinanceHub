package org.example.repaymentservice.service;

import org.example.repaymentservice.model.Schedule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class PenaltyService {
    
    @Value("${repayment.penalty.rate:0.05}")
    private BigDecimal penaltyRate;
    
    @Value("${repayment.penalty.grace-period-days:3}")
    private int gracePeriodDays;
    
    @Value("${repayment.penalty.enabled:true}")
    private boolean penaltyEnabled;
    
    /**
     * Calcule la pénalité à partir d'un objet Schedule
     */
    public BigDecimal calculatePenalty(Schedule schedule) {
        if (!penaltyEnabled) {
            return BigDecimal.ZERO;
        }
        
        LocalDateTime dueDate = schedule.getDueDate();
        LocalDateTime now = LocalDateTime.now();
        
        //  payé à temps ou déjà payé, pas de pénalité
        if (schedule.isPaid() || now.isBefore(dueDate) || now.isEqual(dueDate)) {
            return BigDecimal.ZERO;
        }
        
        //  les jours de retard
        long daysOverdue = ChronoUnit.DAYS.between(dueDate, now);
        
        //  de grâce
        if (daysOverdue <= gracePeriodDays) {
            log.debug("de grâce: {} jours, aucune pénalité", daysOverdue);
            return BigDecimal.ZERO;
        }
        
        long penaltyDays = daysOverdue - gracePeriodDays;
        BigDecimal dailyRate = penaltyRate.divide(new BigDecimal("30"), 10, RoundingMode.HALF_UP);
        BigDecimal penalty = schedule.getDueAmount()
            .multiply(dailyRate)
            .multiply(BigDecimal.valueOf(penaltyDays))
            .setScale(0, RoundingMode.HALF_UP);
        
        log.info("calculée: {} FCFA pour {} jours de retard ({} jours après période de grâce)", 
            penalty, daysOverdue, penaltyDays);
        
        return penalty;
    }
    
    /**
     * Calcule la pénalité à partir des paramètres
     */
    public BigDecimal calculatePenalty(BigDecimal dueAmount, LocalDateTime dueDate, LocalDateTime paymentDate) {
        if (!penaltyEnabled) {
            return BigDecimal.ZERO;
        }
        
        //  payé à temps, pas de pénalité
        if (paymentDate.isBefore(dueDate) || paymentDate.isEqual(dueDate)) {
            return BigDecimal.ZERO;
        }
        
        //  les jours de retard
        long daysOverdue = ChronoUnit.DAYS.between(dueDate, paymentDate);
        
        //  de grâce
        if (daysOverdue <= gracePeriodDays) {
            log.debug("de grâce: {} jours, aucune pénalité", daysOverdue);
            return BigDecimal.ZERO;
        }
        
        long penaltyDays = daysOverdue - gracePeriodDays;
        BigDecimal dailyRate = penaltyRate.divide(new BigDecimal("30"), 10, RoundingMode.HALF_UP);
        BigDecimal penalty = dueAmount
            .multiply(dailyRate)
            .multiply(BigDecimal.valueOf(penaltyDays))
            .setScale(0, RoundingMode.HALF_UP);
        
        log.info("calculée: {} FCFA pour {} jours de retard ({} jours après période de grâce)", 
            penalty, daysOverdue, penaltyDays);
        
        return penalty;
    }
}

//  org.example.repaymentservice.service;

//  org.example.repaymentservice.model.Penalty;
//  org.example.repaymentservice.model.Schedule;
//  org.example.repaymentservice.repository.PenaltyRepository;
//  lombok.RequiredArgsConstructor;
//  lombok.extern.slf4j.Slf4j;
//  org.springframework.beans.factory.annotation.Value;
//  org.springframework.stereotype.Service;

//  java.math.BigDecimal;
//  java.math.RoundingMode;
//  java.time.LocalDateTime;
//  java.time.temporal.ChronoUnit;

// 
// 
// 
//  class PenaltyService {
    
//      final PenaltyRepository penaltyRepository;
    
//     ("${repayment.penalty.rate:0.05}")
//      BigDecimal penaltyRate;
    
//     ("${repayment.penalty.grace-period-days:3}")
//      int gracePeriodDays;
    
//      BigDecimal calculatePenalty(Schedule schedule) {
//         //  si l'échéance est déjà payée
//          (schedule.isPaid()) {
//              BigDecimal.ZERO;
//         
        
//         //  si la date d'échéance est dépassée
//          now = LocalDateTime.now();
//          (!schedule.getDueDate().isBefore(now)) {
//              BigDecimal.ZERO;
//         
        
//         //  les jours de retard
//          daysOverdue = ChronoUnit.DAYS.between(schedule.getDueDate(), now);
        
//         //  de grâce
//          (daysOverdue <= gracePeriodDays) {
//             .debug("de grâce: {} jours de retard, aucune pénalité", daysOverdue);
//              BigDecimal.ZERO;
//         
        
//          penaltyDays = daysOverdue - gracePeriodDays;
//          dailyRate = penaltyRate.divide(new BigDecimal("30"), 10, RoundingMode.HALF_UP);
        
//         //  dueAmount comme montant de référence (comme dans amortization_schedules)
//          penalty = schedule.getDueAmount()
//             .(dailyRate)
//             .(BigDecimal.valueOf(penaltyDays))
//             .(0, RoundingMode.HALF_UP);
        
//         //  la pénalité
//          penaltyEntity = Penalty.builder()
//             .(schedule.getLoanId())
//             .(schedule.getId())
//             .(schedule.getInstallmentNumber())
//             .(penalty)
//             .(penaltyRate)
//             .((int) daysOverdue)
//             .(false)
//             .();
        
//         .save(penaltyEntity);
        
//         .info("calculée pour loanId={}, installment={}: {} FCFA ({} jours de retard, {} jours de pénalité)", 
//             .getLoanId(), schedule.getInstallmentNumber(), penalty, daysOverdue, penaltyDays);
        
//          penalty;
//     
// 