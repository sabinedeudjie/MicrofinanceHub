package org.example.reportingservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import org.example.reportingservice.config.FeignClientConfig;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@FeignClient(name = "REPAYMENT-SERVICE",
             url = "${repayment.service.url:http://localhost:8084}",
             configuration = FeignClientConfig.class)
public interface RepaymentServiceClient {
    
    
    @GetMapping("/api/repayments/stats")
    RepaymentStats getRepaymentStats(
        @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        @RequestHeader("Authorization") String token);

    
    @GetMapping("/api/repayments/agent/{agentId}/total")
    BigDecimal getTotalRepaymentsForAgent(
        @PathVariable("agentId") String agentId,
        @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        @RequestHeader("Authorization") String token);

    
    @PostMapping("/api/repayments/stats/by-clients")
    RepaymentStats getRepaymentStatsForClients(@RequestBody List<String> clientIds,
                                               @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                               @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                                               @RequestHeader("Authorization") String token);

    
    @PostMapping("/api/repayments/stats/by-agent")
    RepaymentStats getRepaymentStatsForAgent(@RequestParam("agentId") String agentId,
                                             @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                             @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                                             @RequestHeader("Authorization") String token);
}