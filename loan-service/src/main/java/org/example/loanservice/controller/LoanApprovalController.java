package org.example.loanservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.example.loanservice.dto.equest.LoanApprovalRequest;
import org.example.loanservice.dto.equest.LoanRejectionRequest;
import org.example.loanservice.dto.response.LoanResponse;
import org.example.loanservice.security.JwtService;
import org.example.loanservice.service.LoanService;

@Slf4j
@RestController
@RequestMapping("/api/loans/approval")
@RequiredArgsConstructor
public class LoanApprovalController {
    
    private final LoanService loanService;
    private final JwtService jwtService;
    
    /**
     * Approuver une demande de prêt
     */
    @PostMapping("/{applicationId}/approve")
    public ResponseEntity<LoanResponse> approveLoan(
        @PathVariable String applicationId,
        @Valid @RequestBody LoanApprovalRequest request,
        @RequestHeader("X-User-Id") String userId,
        @RequestHeader("X-User-Role") String userRole,
        @RequestHeader("Authorization") String token) {
    
        log.info("de la demande: {} par {} (rôle: {})", applicationId, userId, userRole);
        LoanResponse response = loanService.approveLoan(applicationId, userId, userRole, token, request);
        return ResponseEntity.ok(response);
    }
    // ("/{applicationId}/approve")
    //  ResponseEntity<LoanResponse> approveLoan(
    //      String applicationId,
    //      @RequestBody LoanApprovalRequest request,
    //     ("X-User-Id") String userId,
    //     ("X-User-Role") String userRole,
    //     ("Authorization") String token) {
    
    //     .info("de la demande: {} par {} (rôle: {})", applicationId, userId, userRole);
    //      response = loanService.approveLoan(applicationId, userId, userRole, token);
    //      ResponseEntity.ok(response);
    // 
    
    @PostMapping("/{applicationId}/reject")
    public ResponseEntity<Map<String, String>> rejectLoan(
            @PathVariable String applicationId,
            @Valid @RequestBody LoanRejectionRequest request,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader("Authorization") String token) {
        
        // 'email est extrait du token, pas du header
        String userEmail = jwtService.extractEmail(token);
        
        log.info("de la demande: {} par {} (rôle: {})", applicationId, userEmail, userRole);
        
        loanService.rejectLoan(applicationId, request.getRejectionReason(), userEmail, userRole, token);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "REJECTED");
        response.put("message", "Demande rejetée avec succès");
        response.put("applicationId", applicationId);
        response.put("reason", request.getRejectionReason());
        response.put("rejectedBy", userEmail);
        
        return ResponseEntity.ok(response);
    }

/**
 * Décaisser un prêt approuvé
 */
@PostMapping("/{loanId}/disburse")
public ResponseEntity<LoanResponse> disburseLoan(
        @PathVariable String loanId,
        @RequestHeader("X-User-Id") String userId,
        @RequestHeader("X-User-Role") String userRole,  
        @RequestHeader("Authorization") String token) {
    
    log.info("du prêt: {} par {} (rôle: {})", loanId, userId, userRole);
    
    //  userRole et token
    LoanResponse response = loanService.disburseLoan(loanId, userId, userRole, token);
    return ResponseEntity.ok(response);
}
    /**
     * Repasser une demande au statut PENDING (Admin uniquement)
     * Utilisé pour corriger une erreur ou réouvrir une demande
     */
    @PostMapping("/{applicationId}/reset-to-pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<Map<String, String>> resetToPending(
            @PathVariable String applicationId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader("Authorization") String token) {
        
        log.info("de la demande {} au statut PENDING par {} (rôle: {})", applicationId, userId, userRole);
        
        loanService.resetToPending(applicationId, userId, userRole, token);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "La demande a été repassée au statut PENDING");
        response.put("applicationId", applicationId);
        response.put("resetBy", userId);
        
        return ResponseEntity.ok(response);
    }
}