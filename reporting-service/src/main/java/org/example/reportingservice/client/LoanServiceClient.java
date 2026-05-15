package org.example.reportingservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import org.example.reportingservice.config.FeignClientConfig;
import org.example.reportingservice.dto.reponse.LoanApplicationResponse;
import org.example.reportingservice.dto.reponse.LoanResponse;

import java.time.LocalDateTime;
import java.util.List;

@FeignClient(name = "LOAN-SERVICE",
             url = "${loan.service.url:http://localhost:8083}",
             configuration = FeignClientConfig.class)
public interface LoanServiceClient {
    
    
    @GetMapping("/api/loans/stats")
    LoanStats getLoanStats(
        @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        @RequestHeader("Authorization") String token);
    
    
    @GetMapping("/api/loans/portfolio/stats")
    PortfolioStats getPortfolioStats(@RequestHeader("Authorization") String token);

    
    @PostMapping("/api/loans/stats/by-clients")
    LoanStats getLoanStatsForClients(
        @RequestBody List<String> clientIds,
        @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        @RequestHeader("Authorization") String token);
    
    
    @PostMapping("/api/loans/applications/pending/count")
    Long countPendingApplicationsForClients(@RequestBody List<String> clientIds,
                                             @RequestHeader("Authorization") String token);

    
    @GetMapping("/api/loans/client/{clientId}")
    List<LoanResponse> getClientLoans(@PathVariable("clientId") String clientId,
                                       @RequestHeader("Authorization") String token);
    
    
    @GetMapping("/api/loans/{loanId}")
    LoanResponse getLoan(@PathVariable("loanId") String loanId,
                          @RequestHeader("Authorization") String token);

    
    @PostMapping("/api/loans/portfolio/stats/by-clients")
    PortfolioStats getPortfolioStatsForClients(@RequestBody List<String> clientIds,
                                                @RequestHeader("Authorization") String token);

    
    @PostMapping("/api/loans/stats/by-agent")
    LoanStats getLoanStatsForAgent(@RequestParam("agentId") String agentId,
                                   @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                   @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                                   @RequestHeader("Authorization") String token);

    
    @PostMapping("/api/loans/portfolio/stats/by-agent")
    PortfolioStats getPortfolioStatsForAgent(@RequestParam("agentId") String agentId,
                                              @RequestHeader("Authorization") String token);


    /**
     *  Récupérer les demandes en attente avec pagination
     */
    @GetMapping("/api/loans/applications/pending")
    Page<LoanApplicationResponse> getPendingApplications( @RequestParam("page") int page,
                                              @RequestParam("size") int size,
                                              @RequestHeader("Authorization") String token);
}