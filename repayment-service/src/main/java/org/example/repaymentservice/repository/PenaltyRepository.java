package org.example.repaymentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.example.repaymentservice.model.Penalty;

import java.util.List;

@Repository
public interface PenaltyRepository extends JpaRepository<Penalty, String> {
    
    List<Penalty> findByLoanId(String loanId);
    
    List<Penalty> findByScheduleId(String scheduleId);
    
    List<Penalty> findByLoanIdAndPaid(String loanId, boolean paid);
}