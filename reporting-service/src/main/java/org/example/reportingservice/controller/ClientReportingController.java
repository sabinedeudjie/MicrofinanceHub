package org.example.reportingservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.example.reportingservice.dto.reponse.ClientDashboardResponse;
import org.example.reportingservice.service.ReportingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/reporting/client")
@RequiredArgsConstructor
@Slf4j
public class ClientReportingController {
    
    private final ReportingService reportingService;
    
    @GetMapping("/dashboard/{clientId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ClientDashboardResponse> getClientDashboard(
            @PathVariable String clientId,
            @RequestHeader("X-User-Id") String userId) {
        
        if (!clientId.equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        
        return ResponseEntity.ok(reportingService.getClientKpis(clientId));
    }
}
