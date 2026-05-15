package org.example.reportingservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.example.reportingservice.dto.reponse.DashboardResponse;
import org.example.reportingservice.service.ReportingService;

@Slf4j
@RestController
@RequestMapping("/api/reporting")
@RequiredArgsConstructor
public class DashboardController {
    
    private final ReportingService reportingService;
    
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<DashboardResponse> getDashboard() {
        log.info("demandé");
        return ResponseEntity.ok(reportingService.getDashboard());
    }
}

//  org.example.reportingservice.controller;

//  lombok.RequiredArgsConstructor;
//  lombok.extern.slf4j.Slf4j;
//  org.springframework.http.ResponseEntity;
//  org.springframework.security.access.prepost.PreAuthorize;
//  org.springframework.web.bind.annotation.*;

//  org.example.reportingservice.dto.reponse.DashboardResponse;
//  org.example.reportingservice.service.ReportingService;

// 
// 
// ("/api/reporting/dashboard")
// 
//  class DashboardController {
    
//      final ReportingService reportingService;
    
//     /**
//      *  - Accessible aux ADMIN, MANAGER et AGENT
//      * - /MANAGER: voient toutes les données
//      * -  voient les données limitées à leur portefeuille
//      */
//     
//     ("hasAnyRole('ADMIN', 'MANAGER', 'AGENT')")
//      ResponseEntity<DashboardResponse> getDashboard() {
//         .info("demandé");
//          ResponseEntity.ok(reportingService.getDashboard());
//     
// 
