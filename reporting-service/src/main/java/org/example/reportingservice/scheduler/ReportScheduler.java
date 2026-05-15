package org.example.reportingservice.scheduler;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.example.reportingservice.service.KpiCalculationService;
import org.example.reportingservice.service.ReportGenerationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportScheduler {
    
    private final ReportGenerationService reportGenerationService;
    private final KpiCalculationService kpiCalculationService;
    
    @Scheduled(cron = "0 0 8 * * *") //  les jours à 8h
    public void generateDailyReport() {
        log.info("du rapport quotidien");
        reportGenerationService.generateDailyReport();
    }
    
    @Scheduled(cron = "0 0 9 1 * *") //  1er du mois à 9h
    public void generateMonthlyReport() {
        log.info("du rapport mensuel");
        reportGenerationService.generateMonthlyReport();
    }

    @Scheduled(cron = "0 0 1 * * *") //  les jours à 1h du matin
    public void updateKpis() {
        log.info("à jour programmée des KPIs");
        kpiCalculationService.updateAllKpis();
    }
}