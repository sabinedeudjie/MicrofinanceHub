package org.example.repaymentservice.repository;

import org.example.repaymentservice.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, UUID>  {
    
    //  les schedules d'un prêt par ordre croissant
    List<Schedule> findByLoanIdOrderByInstallmentNumberAsc(String loanId);
    
    //  les schedules impayés d'un prêt
    @Query("SELECT s FROM Schedule s WHERE s.loanId = :loanId AND s.paid = false ORDER BY s.dueDate ASC")
    List<Schedule> findPendingSchedulesByLoanId(@Param("loanId") String loanId);
    
    //  les schedules par loanId
    List<Schedule> findByLoanId(String loanId);
    
    //  pour trouver les échéances impayées avec date dépassée
    @Query("SELECT s FROM Schedule s WHERE s.dueDate < :date AND s.paid = false ORDER BY s.dueDate ASC")
    List<Schedule> findByDueDateBeforeAndPaidFalse(@Param("date") LocalDateTime date);
    
    //  avec nommage JPA (si les noms de colonnes sont corrects)
    //  findByDueDateBeforeAndPaidFalse(LocalDateTime date);
    
    //  une échéance spécifique
    Optional<Schedule> findByLoanIdAndInstallmentNumber(String loanId, Integer installmentNumber);
    
    //  les schedules d'un prêt
    long countByLoanId(String loanId);
    
    //  les schedules impayés d'un prêt
    long countByLoanIdAndPaidFalse(String loanId);
}