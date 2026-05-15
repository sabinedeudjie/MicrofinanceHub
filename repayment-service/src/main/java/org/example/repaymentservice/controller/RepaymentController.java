package org.example.repaymentservice.controller;

import org.example.repaymentservice.client.ClientServiceClient;
import org.example.repaymentservice.dto.ClientInfo;
import org.example.repaymentservice.dto.request.AgentPaymentRequest;
import org.example.repaymentservice.dto.request.PaymentRequest;
import org.example.repaymentservice.dto.response.PaymentResponse;
import org.example.repaymentservice.dto.response.RepaymentStats;
import org.example.repaymentservice.repository.RepaymentRepository;
import org.example.repaymentservice.service.RepaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/repayments")
@RequiredArgsConstructor
public class RepaymentController {
    
    private final RepaymentService repaymentService;
    private final RepaymentRepository repaymentRepository;
    private final ClientServiceClient clientServiceClient;
    
    /**
     * Endpoint pour le paiement par le client lui-même
     */
    @PostMapping("/pay/client")
    public ResponseEntity<PaymentResponse> clientMakePayment(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("X-User-Id") String clientIdFromToken) {
        
        log.info("PAIEMENT PAR CLIENT ===");
        PaymentResponse response = repaymentService.clientMakePayment(request, authorization, clientIdFromToken);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Endpoint pour l'enregistrement de paiement par agent/admin/directeur
     */
    @PostMapping("/pay/record")
    public ResponseEntity<PaymentResponse> agentRecordPayment(
            @Valid @RequestBody AgentPaymentRequest request,
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("X-User-Id") String agentId,
            @RequestHeader("X-User-Role") String agentRole) {
        
        log.info("ENREGISTREMENT PAIEMENT PAR {} ===", agentRole);
        log.info("ID: {}", agentId);
        
        PaymentResponse response = repaymentService.agentRecordPayment(request, authorization, agentId, agentRole);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint pour la validation d'un remboursement par un directeur ou admin
     */
    @PostMapping("/{paymentId}/validate")
    public ResponseEntity<PaymentResponse> validatePayment(
            @PathVariable String paymentId,
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        
        log.info("VALIDATION PAIEMENT PAR {} ===", userRole);
        PaymentResponse response = repaymentService.validatePayment(paymentId, authorization, userId, userRole);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint pour récupérer les paiements en attente de validation
     */
    @GetMapping("/pending")
    public ResponseEntity<List<PaymentResponse>> getPendingPayments(
            @RequestHeader("Authorization") String authorization) {
        log.info("RÉCUPÉRATION PAIEMENTS EN ATTENTE ===");
        return ResponseEntity.ok(repaymentService.getPendingPayments(authorization));
    }
    
    /**
     * Endpoint générique qui redirige selon le rôle (alternative)
     */
    @PostMapping("/pay")
    public ResponseEntity<PaymentResponse> makePayment(
            @Valid @RequestBody Object request,
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        
        log.info("PAIEMENT - Rôle: {} ===", userRole);
        
        //  si c'est une requête d'agent (avec clientId)
        if (request instanceof AgentPaymentRequest) {
            AgentPaymentRequest agentRequest = (AgentPaymentRequest) request;
            log.info("d'enregistrement par {}: pour le client {}", userRole, agentRequest.getClientId());
            PaymentResponse response = repaymentService.agentRecordPayment(agentRequest, authorization, userId, userRole);
            return ResponseEntity.ok(response);
        } 
        // , c'est un paiement client
        else if (request instanceof PaymentRequest) {
            PaymentRequest clientRequest = (PaymentRequest) request;
            log.info("de paiement par le client: {}", userId);
            PaymentResponse response = repaymentService.clientMakePayment(clientRequest, authorization, userId);
            return ResponseEntity.ok(response);
        }
        
        throw new RuntimeException("Format de requête invalide");
    }
    
    @GetMapping("/loan/{loanId}")
    public ResponseEntity<?> getRepaymentStatus(@PathVariable String loanId,
                                                 @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(repaymentService.getRepaymentStatus(loanId, authorization));
    }

    /**
     * Endpoint pour les statistiques de remboursements (utilisé par le Reporting Service)
     */
    @GetMapping("/stats")
    public ResponseEntity<RepaymentStats> getRepaymentStats(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        RepaymentStats stats = repaymentService.getRepaymentStats(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    // 
    //  REMBOURSEMENTS POUR AGENT
    // 

    /**
     * Statistiques des remboursements pour les clients d'un agent
     */
    @PostMapping("/stats/by-agent")
    public ResponseEntity<RepaymentStats> getRepaymentStatsForAgent(
            @RequestParam String agentId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestHeader("Authorization") String authorization) {
        
        log.info("des statistiques remboursements pour l'agent: {}", agentId);
        
        List<String> clientIds = getClientIdsByAgent(agentId, authorization);
        
        if (clientIds.isEmpty()) {
            return ResponseEntity.ok(createEmptyRepaymentStats());
        }
        
        RepaymentStats stats = repaymentService.getRepaymentStatsForClients(clientIds, startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    /**
     * Total des remboursements pour un agent
     */
    @GetMapping("/by-agent/{agentId}/total")
    public ResponseEntity<BigDecimal> getTotalRepaymentsForAgent(
            @PathVariable String agentId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestHeader("Authorization") String authorization) {
        
        log.info("des remboursements pour l'agent: {}", agentId);
        
        List<String> clientIds = getClientIdsByAgent(agentId, authorization);
        
        if (clientIds.isEmpty()) {
            return ResponseEntity.ok(BigDecimal.ZERO);
        }
        
        BigDecimal total = repaymentService.getTotalRepaymentsForClients(clientIds, startDate, endDate);
        return ResponseEntity.ok(total);
    }

    /**
     * Récupère les IDs des clients pour un agent donné
     */
    private List<String> getClientIdsByAgent(String agentId, String authorization) {
        log.info("des IDs clients pour l'agent: {}", agentId);
        
        try {
            List<ClientInfo> clients = clientServiceClient.getMyClients(agentId, authorization);
            
            if (clients != null && !clients.isEmpty()) {
                return clients.stream()
                    .map(ClientInfo::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("lors de l'appel au Client Service: {}", e.getMessage());
        }
        
        return List.of();
    }

    /**
     * Crée des statistiques de remboursement vides
     */
    private RepaymentStats createEmptyRepaymentStats() {
        RepaymentStats stats = new RepaymentStats();
        stats.setTotalRepayments(BigDecimal.ZERO);
        stats.setTotalTransactions(0L);
        stats.setOverdueAmount(BigDecimal.ZERO);
        stats.setOverdueCount(0L);
        stats.setRepaymentRate(0.0);
        return stats;
    }

    // 
    //  POUR LISTE DE CLIENTS
    // 
    
    /**
     * Statistiques de remboursements pour une liste de clients
     */
    @PostMapping("/stats/by-clients")
    public ResponseEntity<RepaymentStats> getRepaymentStatsForClients(
            @RequestBody List<String> clientIds,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("de remboursements pour {} clients", clientIds.size());
        RepaymentStats stats = repaymentService.getRepaymentStatsForClients(clientIds, startDate, endDate);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Total des remboursements pour une liste de clients
     */
    @PostMapping("/total/by-clients")
    public ResponseEntity<BigDecimal> getTotalRepaymentsForClients(
            @RequestBody List<String> clientIds,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("des remboursements pour {} clients", clientIds.size());
        BigDecimal total = repaymentService.getTotalRepaymentsForClients(clientIds, startDate, endDate);
        return ResponseEntity.ok(total);
    }
}