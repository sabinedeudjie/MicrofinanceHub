package org.example.reportingservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.example.reportingservice.dto.reponse.LoanReportResponse;
import org.example.reportingservice.service.ReportingService;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/reporting/reports")
@RequiredArgsConstructor
public class ReportController {
    
    private final ReportingService reportingService;
    
    @GetMapping("/loans")
    public ResponseEntity<LoanReportResponse> getLoanReport(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        return ResponseEntity.ok(reportingService.generateLoanReport(startDate, endDate));
    }
}