package org.example.reportingservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.example.reportingservice.dto.reponse.KpiResponse;
import org.example.reportingservice.service.ReportingService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/reporting/kpis")
@RequiredArgsConstructor
public class KpiController {
    
    private final ReportingService reportingService;
    
    /**
     * KPIs globaux - Accessibles aux ADMIN, MANAGER et AGENT
     * - ADMIN/MANAGER: tous les KPIs
     * - AGENT: KPIs limités à leur périmètre
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
    public ResponseEntity<List<KpiResponse>> getKpis(
            @RequestParam(required = false) String category) {
        
        log.info("demandés - Catégorie: {}", category != null ? category : "TOUTES");
        return ResponseEntity.ok(reportingService.getKpis(category));
    }
    
    /**
     * KPIs spécifiques pour les clients - Données personnelles
     */
    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<KpiResponse>> getClientKpis(@PathVariable String clientId,
                                                           @RequestHeader("X-User-Id") String userId) {
        log.info("personnels demandés par client: {}", clientId);
        
        //  que le client demande ses propres données
        if (!clientId.equals(userId)) {
            log.warn("{} tente d'accéder aux données de {}", userId, clientId);
            return ResponseEntity.status(403).build();
        }
        
        return ResponseEntity.ok(reportingService.getKpis("CLIENT_" + clientId));
    }
    
}


//  org.example.reportingservice.controller;


//  lombok.RequiredArgsConstructor;
//  org.springframework.http.ResponseEntity;
//  org.springframework.web.bind.annotation.*;

//  org.example.reportingservice.dto.reponse.KpiResponse;
//  org.example.reportingservice.service.ReportingService;

//  java.util.List;

// 
// ("/api/reporting/kpis")
// 
//  class KpiController {
    
//      final ReportingService reportingService;
    
//     
//      ResponseEntity<List<KpiResponse>> getKpis(
//         (required = false) String category
//     ) 
//          ResponseEntity.ok(reportingService.getKpis(category));
//     
// 