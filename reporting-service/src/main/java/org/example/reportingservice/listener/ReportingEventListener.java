package org.example.reportingservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import org.example.reportingservice.config.RabbitMQConfig;
import org.example.reportingservice.service.KpiCalculationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportingEventListener {

    private final KpiCalculationService kpiCalculationService;

    @RabbitListener(queues = RabbitMQConfig.REPORTING_LOAN_QUEUE)
    public void handleLoanEvent(String event) {
        log.info("prêt reçu pour reporting");
        kpiCalculationService.updateLoanKpis();
    }

    @RabbitListener(queues = RabbitMQConfig.REPORTING_REPAYMENT_QUEUE)
    public void handleRepaymentEvent(String event) {
        log.info("remboursement reçu pour reporting");
        kpiCalculationService.updateRepaymentKpis();
    }
}
