package org.example.repaymentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.example.repaymentservice.model.Schedule;
import org.example.repaymentservice.repository.ScheduleRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OverdueService {
    
    private final ScheduleRepository scheduleRepository;
    private final PenaltyService penaltyService;
    
    @Transactional
    public void checkAndUpdateOverdueSchedules() {
        //  les échéances non payées dont la date est dépassée
        List<Schedule> overdueSchedules = scheduleRepository.findByDueDateBeforeAndPaidFalse(
            LocalDateTime.now());
        
        for (Schedule schedule : overdueSchedules) {
            log.info("en retard détectée: loanId={}, installment={}, dueDate={}", 
                schedule.getLoanId(), schedule.getInstallmentNumber(), schedule.getDueDate());
            
            //  les pénalités
            penaltyService.calculatePenalty(schedule);
        }
    }
}