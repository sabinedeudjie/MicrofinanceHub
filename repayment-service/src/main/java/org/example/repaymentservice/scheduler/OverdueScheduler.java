package org.example.repaymentservice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.example.repaymentservice.service.OverdueService;

@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueScheduler {
    
    private final OverdueService overdueService;
    
    @Scheduled(cron = "0 0 1 * * *")  //  les jours à 1h du matin
    public void checkOverduePayments() {
        log.info("des paiements en retard...");
        overdueService.checkAndUpdateOverdueSchedules();
    }
}