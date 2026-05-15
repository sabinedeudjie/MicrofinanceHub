package org.example.loanservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.example.loanservice.model.AmortizationSchedule;
import org.example.loanservice.model.Loan;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AmortizationScheduleRepository extends JpaRepository<AmortizationSchedule, String> {
    
    List<AmortizationSchedule> findByLoanOrderByInstallmentNumberAsc(Loan loan);
    
    Optional<AmortizationSchedule> findByLoanAndInstallmentNumber(Loan loan, Integer installmentNumber);
    
    List<AmortizationSchedule> findByLoanAndPaidFalseAndDueDateBefore(Loan loan, LocalDateTime date);
    
    Long countByLoanAndPaidFalse(Loan loan);
}