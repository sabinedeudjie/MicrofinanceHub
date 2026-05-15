package org.example.reportingservice.service;

import org.example.reportingservice.client.LoanServiceClient;
import org.example.reportingservice.client.LoanStats;
import org.example.reportingservice.client.ClientServiceClient;
import org.example.reportingservice.client.ClientStats;
import org.example.reportingservice.client.RepaymentServiceClient;
import org.example.reportingservice.client.RepaymentStats;
import org.example.reportingservice.exporter.ExcelExporter;
import org.example.reportingservice.exporter.PdfExporter;
import org.example.reportingservice.model.Report;
import org.example.reportingservice.model.enums.ReportFormat;
import org.example.reportingservice.model.enums.ReportType;
import org.example.reportingservice.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerationService {
    
    private final LoanServiceClient loanServiceClient;
    private final ClientServiceClient clientServiceClient;
    private final RepaymentServiceClient repaymentServiceClient;
    private final ReportRepository reportRepository;
    private final PdfExporter pdfExporter;
    private final ExcelExporter excelExporter;
    
    private static final String REPORTS_DIRECTORY = "reports/";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    /**
     * Récupère le token JWT depuis le contexte Spring Security
     */
    private String getAuthToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() instanceof String) {
            String token = (String) authentication.getCredentials();
            if (token != null && !token.startsWith("Bearer ")) {
                return "Bearer " + token;
            }
            return token;
        }
        return null;
    }
    
    @Transactional
    public void generateDailyReport() {
        log.info("du rapport quotidien");
        
        String token = getAuthToken();
        if (token == null) {
            log.warn("token trouvé, impossible de générer le rapport quotidien");
            return;
        }
        
        try {
            LocalDateTime startDate = LocalDateTime.now().minusDays(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime endDate = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            
            //  les données du jour avec token
            ClientStats clientStats = clientServiceClient.getClientStats(token);
            LoanStats loanStats = loanServiceClient.getLoanStats(startDate, endDate, token);
            RepaymentStats repaymentStats = repaymentServiceClient.getRepaymentStats(startDate, endDate, token);
            
            //  les données du rapport
            Map<String, Object> reportData = new HashMap<>();
            reportData.put("reportDate", LocalDateTime.now());
            reportData.put("period", "Quotidien");
            reportData.put("startDate", startDate);
            reportData.put("endDate", endDate);
            
            //  les statistiques clients
            if (clientStats != null) {
                reportData.put("newClients", clientStats.getNewClientsThisMonth() != null ? clientStats.getNewClientsThisMonth() : 0L);
                reportData.put("totalClients", clientStats.getTotalClients() != null ? clientStats.getTotalClients() : 0L);
                reportData.put("activeClients", clientStats.getActiveClients() != null ? clientStats.getActiveClients() : 0L);
            }
            
            //  les statistiques de prêts
            if (loanStats != null) {
                reportData.put("newLoanApplications", loanStats.getTotalApplications() != null ? loanStats.getTotalApplications() : 0L);
                reportData.put("approvedLoans", loanStats.getApprovedApplications() != null ? loanStats.getApprovedApplications() : 0L);
                reportData.put("disbursedLoans", loanStats.getDisbursedLoans() != null ? loanStats.getDisbursedLoans() : 0L);
                reportData.put("totalDisbursedAmount", loanStats.getTotalDisbursedAmount() != null ? loanStats.getTotalDisbursedAmount() : java.math.BigDecimal.ZERO);
            }
            
            //  les statistiques de remboursements
            if (repaymentStats != null) {
                reportData.put("totalRepayments", repaymentStats.getTotalRepayments() != null ? repaymentStats.getTotalRepayments() : java.math.BigDecimal.ZERO);
                reportData.put("repaymentRate", repaymentStats.getRepaymentRate() != null ? repaymentStats.getRepaymentRate() : 0.0);
                reportData.put("overdueAmount", repaymentStats.getOverdueAmount() != null ? repaymentStats.getOverdueAmount() : java.math.BigDecimal.ZERO);
            }
            
            //  le fichier Excel
            String fileName = "daily_report_" + LocalDateTime.now().format(DATE_FORMATTER);
            String excelPath = excelExporter.exportToExcel(reportData, fileName);
            
            //  le fichier PDF
            String pdfPath = pdfExporter.exportToPdf(reportData, fileName);
            
            //  les références des rapports
            saveReportReference("Rapport Quotidien", ReportType.DAILY, ReportFormat.EXCEL, excelPath);
            saveReportReference("Rapport Quotidien", ReportType.DAILY, ReportFormat.PDF, pdfPath);
            
            log.info("quotidien généré avec succès: {}", fileName);
            
        } catch (Exception e) {
            log.error("lors de la génération du rapport quotidien: {}", e.getMessage(), e);
        }
    }
    
    @Transactional
    public void generateMonthlyReport() {
        log.info("du rapport mensuel");
        
        String token = getAuthToken();
        if (token == null) {
            log.warn("token trouvé, impossible de générer le rapport mensuel");
            return;
        }
        
        try {
            LocalDateTime startDate = LocalDateTime.now().minusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0);
            LocalDateTime endDate = LocalDateTime.now().withDayOfMonth(1).minusDays(1).withHour(23).withMinute(59);
            
            //  les données du mois avec token
            ClientStats clientStats = clientServiceClient.getClientStats(token);
            LoanStats loanStats = loanServiceClient.getLoanStats(startDate, endDate, token);
            RepaymentStats repaymentStats = repaymentServiceClient.getRepaymentStats(startDate, endDate, token);
            LoanStats monthlyLoanStats = loanServiceClient.getLoanStats(startDate, endDate, token);
            
            //  les données du rapport
            Map<String, Object> reportData = new HashMap<>();
            reportData.put("reportDate", LocalDateTime.now());
            reportData.put("period", "Mensuel");
            reportData.put("month", startDate.getMonth().toString());
            reportData.put("year", startDate.getYear());
            reportData.put("startDate", startDate);
            reportData.put("endDate", endDate);
            
            //  les statistiques clients
            if (clientStats != null) {
                reportData.put("totalClients", clientStats.getTotalClients() != null ? clientStats.getTotalClients() : 0L);
                reportData.put("newClients", clientStats.getNewClientsThisMonth() != null ? clientStats.getNewClientsThisMonth() : 0L);
                reportData.put("activeClients", clientStats.getActiveClients() != null ? clientStats.getActiveClients() : 0L);
                reportData.put("clientGrowthRate", clientStats.getClientGrowthRate() != null ? clientStats.getClientGrowthRate() : 0.0);
            }
            
            //  les statistiques de prêts
            if (loanStats != null) {
                reportData.put("totalLoanApplications", loanStats.getTotalApplications() != null ? loanStats.getTotalApplications() : 0L);
                reportData.put("approvedLoans", loanStats.getApprovedApplications() != null ? loanStats.getApprovedApplications() : 0L);
                reportData.put("rejectedLoans", loanStats.getRejectedApplications() != null ? loanStats.getRejectedApplications() : 0L);
                reportData.put("disbursedLoans", loanStats.getDisbursedLoans() != null ? loanStats.getDisbursedLoans() : 0L);
                reportData.put("activeLoans", loanStats.getActiveLoans() != null ? loanStats.getActiveLoans() : 0L);
                reportData.put("completedLoans", loanStats.getCompletedLoans() != null ? loanStats.getCompletedLoans() : 0L);
                reportData.put("defaultedLoans", loanStats.getDefaultedLoans() != null ? loanStats.getDefaultedLoans() : 0L);
                reportData.put("totalDisbursedAmount", loanStats.getTotalDisbursedAmount() != null ? loanStats.getTotalDisbursedAmount() : java.math.BigDecimal.ZERO);
                reportData.put("outstandingAmount", loanStats.getOutstandingAmount() != null ? loanStats.getOutstandingAmount() : java.math.BigDecimal.ZERO);
                reportData.put("approvalRate", loanStats.getApprovalRate() != null ? loanStats.getApprovalRate() : 0.0);
                reportData.put("defaultRate", loanStats.getDefaultRate() != null ? loanStats.getDefaultRate() : 0.0);
            }
            
            //  les statistiques de remboursements
            if (repaymentStats != null) {
                reportData.put("totalRepayments", repaymentStats.getTotalRepayments() != null ? repaymentStats.getTotalRepayments() : java.math.BigDecimal.ZERO);
                reportData.put("totalTransactions", repaymentStats.getTotalTransactions() != null ? repaymentStats.getTotalTransactions() : 0L);
                reportData.put("repaymentRate", repaymentStats.getRepaymentRate() != null ? repaymentStats.getRepaymentRate() : 0.0);
                reportData.put("overdueAmount", repaymentStats.getOverdueAmount() != null ? repaymentStats.getOverdueAmount() : java.math.BigDecimal.ZERO);
                reportData.put("overdueCount", repaymentStats.getOverdueCount() != null ? repaymentStats.getOverdueCount() : 0L);
            }
            
            //  les tendances mensuelles
            if (monthlyLoanStats != null) {
                reportData.put("monthlyLoanGrowth", calculateMonthlyGrowth(monthlyLoanStats));
            }
            
            //  les fichiers
            String fileName = "monthly_report_" + startDate.getYear() + "_" + startDate.getMonthValue() + "_" + LocalDateTime.now().format(DATE_FORMATTER);
            String excelPath = excelExporter.exportToExcel(reportData, fileName);
            String pdfPath = pdfExporter.exportToPdf(reportData, fileName);
            
            //  les références
            saveReportReference("Rapport Mensuel " + startDate.getMonth() + " " + startDate.getYear(), 
                ReportType.MONTHLY, ReportFormat.EXCEL, excelPath);
            saveReportReference("Rapport Mensuel " + startDate.getMonth() + " " + startDate.getYear(), 
                ReportType.MONTHLY, ReportFormat.PDF, pdfPath);
            
            log.info("mensuel généré avec succès: {}", fileName);
            
        } catch (Exception e) {
            log.error("lors de la génération du rapport mensuel: {}", e.getMessage(), e);
        }
    }
    
    @Transactional
    public void generateWeeklyReport() {
        log.info("du rapport hebdomadaire");
        
        String token = getAuthToken();
        if (token == null) {
            log.warn("token trouvé, impossible de générer le rapport hebdomadaire");
            return;
        }
        
        try {
            LocalDateTime startDate = LocalDateTime.now().minusWeeks(1).withHour(0).withMinute(0);
            LocalDateTime endDate = LocalDateTime.now().withHour(23).withMinute(59);
            
            LoanStats loanStats = loanServiceClient.getLoanStats(startDate, endDate, token);
            RepaymentStats repaymentStats = repaymentServiceClient.getRepaymentStats(startDate, endDate, token);
            
            Map<String, Object> reportData = new HashMap<>();
            reportData.put("reportDate", LocalDateTime.now());
            reportData.put("period", "Hebdomadaire");
            reportData.put("startDate", startDate);
            reportData.put("endDate", endDate);
            
            if (loanStats != null) {
                reportData.put("newLoans", loanStats.getTotalApplications());
                reportData.put("disbursedAmount", loanStats.getTotalDisbursedAmount());
            }
            
            if (repaymentStats != null) {
                reportData.put("collectedAmount", repaymentStats.getTotalRepayments());
                reportData.put("collectionRate", repaymentStats.getRepaymentRate());
            }
            
            String fileName = "weekly_report_" + LocalDateTime.now().format(DATE_FORMATTER);
            String pdfPath = pdfExporter.exportToPdf(reportData, fileName);
            
            saveReportReference("Rapport Hebdomadaire", ReportType.WEEKLY, ReportFormat.PDF, pdfPath);
            
            log.info("hebdomadaire généré avec succès");
            
        } catch (Exception e) {
            log.error("lors de la génération du rapport hebdomadaire: {}", e.getMessage(), e);
        }
    }
    
    @Transactional
    public void generateQuarterlyReport() {
        log.info("du rapport trimestriel");
        
        String token = getAuthToken();
        if (token == null) {
            log.warn("token trouvé, impossible de générer le rapport trimestriel");
            return;
        }
        
        try {
            LocalDateTime startDate = LocalDateTime.now().minusMonths(3).withDayOfMonth(1).withHour(0).withMinute(0);
            LocalDateTime endDate = LocalDateTime.now().withHour(23).withMinute(59);
            
            LoanStats loanStats = loanServiceClient.getLoanStats(startDate, endDate, token);
            ClientStats clientStats = clientServiceClient.getClientStats(token);
            RepaymentStats repaymentStats = repaymentServiceClient.getRepaymentStats(startDate, endDate, token);
            
            Map<String, Object> reportData = new HashMap<>();
            reportData.put("reportDate", LocalDateTime.now());
            reportData.put("period", "Trimestriel");
            reportData.put("quarter", (LocalDateTime.now().getMonthValue() - 1) / 3 + 1);
            reportData.put("year", LocalDateTime.now().getYear());
            reportData.put("startDate", startDate);
            reportData.put("endDate", endDate);
            
            if (clientStats != null) {
                reportData.put("totalClients", clientStats.getTotalClients());
                reportData.put("clientGrowth", clientStats.getClientGrowthRate());
            }
            
            if (loanStats != null) {
                reportData.put("totalLoans", loanStats.getTotalApplications());
                reportData.put("activePortfolio", loanStats.getOutstandingAmount());
                reportData.put("defaultRate", loanStats.getDefaultRate());
            }
            
            if (repaymentStats != null) {
                reportData.put("totalCollected", repaymentStats.getTotalRepayments());
                reportData.put("recoveryRate", repaymentStats.getRepaymentRate());
            }
            
            String fileName = "quarterly_report_Q" + ((LocalDateTime.now().getMonthValue() - 1) / 3 + 1) + 
                "_" + LocalDateTime.now().getYear() + "_" + LocalDateTime.now().format(DATE_FORMATTER);
            
            String excelPath = excelExporter.exportToExcel(reportData, fileName);
            String pdfPath = pdfExporter.exportToPdf(reportData, fileName);
            
            saveReportReference("Rapport Trimestriel Q" + ((LocalDateTime.now().getMonthValue() - 1) / 3 + 1), 
                ReportType.QUARTERLY, ReportFormat.EXCEL, excelPath);
            saveReportReference("Rapport Trimestriel Q" + ((LocalDateTime.now().getMonthValue() - 1) / 3 + 1), 
                ReportType.QUARTERLY, ReportFormat.PDF, pdfPath);
            
            log.info("trimestriel généré avec succès");
            
        } catch (Exception e) {
            log.error("lors de la génération du rapport trimestriel: {}", e.getMessage(), e);
        }
    }
    
    @Transactional
    public void generateAnnualReport() {
        log.info("du rapport annuel");
        
        String token = getAuthToken();
        if (token == null) {
            log.warn("token trouvé, impossible de générer le rapport annuel");
            return;
        }
        
        try {
            LocalDateTime startDate = LocalDateTime.now().withDayOfYear(1).withHour(0).withMinute(0);
            LocalDateTime endDate = LocalDateTime.now().withHour(23).withMinute(59);
            
            LoanStats loanStats = loanServiceClient.getLoanStats(startDate, endDate, token);
            ClientStats clientStats = clientServiceClient.getClientStats(token);
            RepaymentStats repaymentStats = repaymentServiceClient.getRepaymentStats(startDate, endDate, token);
            
            Map<String, Object> reportData = new HashMap<>();
            reportData.put("reportDate", LocalDateTime.now());
            reportData.put("period", "Annuel");
            reportData.put("year", LocalDateTime.now().getYear());
            reportData.put("startDate", startDate);
            reportData.put("endDate", endDate);
            
            if (clientStats != null) {
                reportData.put("totalClients", clientStats.getTotalClients());
                reportData.put("newClientsYear", clientStats.getNewClientsThisMonth());
                reportData.put("activeClients", clientStats.getActiveClients());
            }
            
            if (loanStats != null) {
                reportData.put("totalLoanApplications", loanStats.getTotalApplications());
                reportData.put("totalDisbursed", loanStats.getTotalDisbursedAmount());
                reportData.put("portfolioOutstanding", loanStats.getOutstandingAmount());
                reportData.put("defaultRate", loanStats.getDefaultRate());
                reportData.put("recoveryRate", loanStats.getRecoveryRate());
            }
            
            if (repaymentStats != null) {
                reportData.put("totalRepayments", repaymentStats.getTotalRepayments());
                reportData.put("collectionRate", repaymentStats.getRepaymentRate());
            }
            
            String fileName = "annual_report_" + LocalDateTime.now().getYear() + "_" + LocalDateTime.now().format(DATE_FORMATTER);
            String excelPath = excelExporter.exportToExcel(reportData, fileName);
            String pdfPath = pdfExporter.exportToPdf(reportData, fileName);
            
            saveReportReference("Rapport Annuel " + LocalDateTime.now().getYear(), 
                ReportType.ANNUAL, ReportFormat.EXCEL, excelPath);
            saveReportReference("Rapport Annuel " + LocalDateTime.now().getYear(), 
                ReportType.ANNUAL, ReportFormat.PDF, pdfPath);
            
            log.info("annuel généré avec succès");
            
        } catch (Exception e) {
            log.error("lors de la génération du rapport annuel: {}", e.getMessage(), e);
        }
    }
    
    private void saveReportReference(String name, ReportType type, ReportFormat format, String filePath) {
        try {
            Path path = Paths.get(filePath);
            long fileSize = Files.exists(path) ? Files.size(path) : 0L;
            
            Report report = Report.builder()
                .name(name)
                .description("Rapport généré automatiquement")
                .type(type)
                .format(format)
                .startDate(type == ReportType.DAILY ? LocalDateTime.now().minusDays(1) : 
                          type == ReportType.WEEKLY ? LocalDateTime.now().minusWeeks(1) :
                          type == ReportType.MONTHLY ? LocalDateTime.now().minusMonths(1) :
                          type == ReportType.QUARTERLY ? LocalDateTime.now().minusMonths(3) :
                          LocalDateTime.now().withDayOfYear(1))
                .endDate(LocalDateTime.now())
                .filePath(filePath)
                .fileSize(fileSize)
                .generatedBy("SYSTEM")
                .generatedAt(LocalDateTime.now())
                .scheduled(true)
                .build();
            
            reportRepository.save(report);
            log.info("du rapport sauvegardée: {}", name);
            
        } catch (IOException e) {
            log.error("lors de la sauvegarde de la référence du rapport: {}", e.getMessage());
        }
    }
    
    private double calculateMonthlyGrowth(LoanStats currentMonthStats) {
        //  implémenter avec les données du mois précédent
        //  l'instant, retourne une valeur par défaut
        return 0.0;
    }
    
    //  utilitaire pour créer le dossier des rapports s'il n'existe pas
    public void ensureReportsDirectoryExists() {
        File directory = new File(REPORTS_DIRECTORY);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                log.info("des rapports créé: {}", REPORTS_DIRECTORY);
            }
        }
    }
}