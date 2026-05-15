package org.example.repaymentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import org.example.repaymentservice.dto.response.AmortizationResponse;
import org.example.repaymentservice.dto.response.LoanInfo;

@FeignClient(name = "LOAN-SERVICE", url = "${loan.service.url:http://localhost:8083}")
public interface LoanServiceClient {
    
    /**
     * Récupère le plan d'amortissement d'un prêt
     */
    @GetMapping("/api/loans/{loanId}/amortization")
    AmortizationResponse getLoanAmortization(
            @PathVariable("loanId") String loanId,
            @RequestHeader("Authorization") String authorization);
    
    /**
     * Récupère l'ID du client associé à un prêt
     */
    @GetMapping("/api/loans/{loanId}/client-id")
    String getClientIdByLoanId(
            @PathVariable("loanId") String loanId,
            @RequestHeader("Authorization") String authorization);
    
    /**
     * Marque une échéance comme payée
     */
    @PutMapping("/api/loans/{loanId}/schedules/{installmentNumber}/pay")
    void markScheduleAsPaid(
            @PathVariable("loanId") String loanId,
            @PathVariable("installmentNumber") Integer installmentNumber,
            @RequestParam("paymentId") String paymentId,
            @RequestHeader("Authorization") String authorization);

    /**
     * Récupère le statut d'un prêt
     */
    @GetMapping("/api/loans/{loanId}/status")
    String getLoanStatus(
            @PathVariable("loanId") String loanId, 
            @RequestHeader("Authorization") String authorization);
    
    /**
     * Vérifie si un prêt existe
     */
    @GetMapping("/api/loans/{loanId}/exists")
    Boolean loanExists(
            @PathVariable("loanId") String loanId,
            @RequestHeader("Authorization") String authorization);
    
    /**
     * Récupère les détails d'un prêt
     */
    @GetMapping("/api/loans/{loanId}")
    LoanInfo getLoanInfo(
            @PathVariable("loanId") String loanId,
            @RequestHeader("Authorization") String authorization);
}