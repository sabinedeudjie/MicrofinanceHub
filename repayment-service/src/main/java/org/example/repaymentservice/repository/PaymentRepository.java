package org.example.repaymentservice.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.example.repaymentservice.model.Payment;
import org.example.repaymentservice.model.enums.PaymentStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    
    Optional<Payment> findByPaymentNumber(String paymentNumber);
    
    List<Payment> findByLoanId(String loanId);
    
    List<Payment> findByLoanIdAndStatus(String loanId, PaymentStatus status);
    
    List<Payment> findByPaymentDateBetween(LocalDateTime start, LocalDateTime end);
    
    List<Payment> findByLoanIdOrderByPaymentDateDesc(String loanId);
    
    List<Payment> findByClientId(String clientId);
    
    List<Payment> findByStatus(PaymentStatus status);
}