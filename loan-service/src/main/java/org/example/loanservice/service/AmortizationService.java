package org.example.loanservice.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.example.loanservice.dto.response.AmortizationEntryResponse;
import org.example.loanservice.dto.response.AmortizationResponse;
import org.example.loanservice.model.AmortizationSchedule;
import org.example.loanservice.model.Loan;
import org.example.loanservice.repository.AmortizationScheduleRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmortizationService {
    
    private final AmortizationScheduleRepository amortizationScheduleRepository;
    
    @Transactional
    public void generateAmortizationSchedule(Loan loan) {
        log.info("du plan d'amortissement pour le prêt: {}", loan.getLoanNumber());
        
        List<AmortizationSchedule> schedule = new ArrayList<>();
        BigDecimal remainingBalance = loan.getAmount();
        BigDecimal monthlyRate = loan.getInterestRate()
            .divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);
        
        LocalDateTime dueDate = loan.getApprovalDate().plusMonths(1);
        
        for (int i = 1; i <= loan.getTermMonths(); i++) {
            BigDecimal interestAmount = remainingBalance.multiply(monthlyRate);
            BigDecimal principalAmount = loan.getMonthlyPayment().subtract(interestAmount);
            
            if (i == loan.getTermMonths()) {
                //  mois: ajuster pour que le solde soit zéro
                principalAmount = remainingBalance;
            }
            
            remainingBalance = remainingBalance.subtract(principalAmount);
            if (remainingBalance.compareTo(BigDecimal.ZERO) < 0) {
                remainingBalance = BigDecimal.ZERO;
            }
            
            AmortizationSchedule entry = AmortizationSchedule.builder()
                .loan(loan)
                .installmentNumber(i)
                .dueDate(dueDate)
                .dueAmount(loan.getMonthlyPayment())
                .principalAmount(principalAmount)
                .interestAmount(interestAmount)
                .remainingBalance(remainingBalance)
                .paid(false)
                .build();
            
            schedule.add(entry);
            dueDate = dueDate.plusMonths(1);
        }
        
        amortizationScheduleRepository.saveAll(schedule);
    }
    
    public AmortizationResponse getAmortizationSchedule(Loan loan) {
        List<AmortizationSchedule> schedule = amortizationScheduleRepository.findByLoanOrderByInstallmentNumberAsc(loan);
        
        return AmortizationResponse.builder()
            .loanId(loan.getId())
            .loanNumber(loan.getLoanNumber())
            .totalAmount(loan.getAmount())
            .totalInterest(loan.getTotalRepayment().subtract(loan.getAmount()))
            .totalRepayment(loan.getTotalRepayment())
            .entries(schedule.stream().map(this::mapToEntryResponse).collect(Collectors.toList()))
            .build();
    }
    
    public List<AmortizationEntryResponse> getScheduleEntries(Loan loan) {
        return amortizationScheduleRepository.findByLoanOrderByInstallmentNumberAsc(loan)
            .stream()
            .map(this::mapToEntryResponse)
            .collect(Collectors.toList());
    }
    
    private AmortizationEntryResponse mapToEntryResponse(AmortizationSchedule schedule) {
        return AmortizationEntryResponse.builder()
            .installmentNumber(schedule.getInstallmentNumber())
            .dueDate(schedule.getDueDate())
            .dueAmount(schedule.getDueAmount())
            .principalAmount(schedule.getPrincipalAmount())
            .interestAmount(schedule.getInterestAmount())
            .remainingBalance(schedule.getRemainingBalance())
            .paid(schedule.isPaid())
            .build();
    }
}