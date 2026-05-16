package org.example.loanservice.repository;

import org.example.loanservice.model.AmortizationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<AmortizationSchedule, String> {
    
    // findByLoanIdOrderByInstallmentNumberAsc(String loanId);
    
    @Query("SELECT s FROM AmortizationSchedule s WHERE s.loan.id = :loanId ORDER BY s.installmentNumber ASC")
    List<AmortizationSchedule> findByLoanIdOrderByInstallmentNumberAsc(@Param("loanId") String loanId);
    
    Optional<AmortizationSchedule> findByLoanIdAndInstallmentNumber(String loanId, Integer installmentNumber);
    
    long countByLoanId(String loanId);
    
    @Modifying
    @Query("DELETE FROM AmortizationSchedule s WHERE s.loan.id = :loanId")
    void deleteByLoanId(@Param("loanId") String loanId);
}