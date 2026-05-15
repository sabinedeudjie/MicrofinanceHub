package org.example.reportingservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.example.reportingservice.dto.reponse.LoanReportResponse;
import org.example.reportingservice.service.ExportService;
import org.example.reportingservice.service.ReportingService;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/reporting/export")
@RequiredArgsConstructor
public class ExportController {

    private final ReportingService reportingService;
    private final ExportService exportService;

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) throws Exception {
        
        LoanReportResponse report = reportingService.generateLoanReport(startDate, endDate);
        byte[] pdfData = exportService.exportToPdf(report);
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rapport_prets.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdfData);
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) throws Exception {
        
        LoanReportResponse report = reportingService.generateLoanReport(startDate, endDate);
        byte[] excelData = exportService.exportToExcel(report);
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rapport_prets.xlsx")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(excelData);
    }
}