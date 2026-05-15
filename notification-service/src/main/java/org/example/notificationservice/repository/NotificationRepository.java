package org.example.notificationservice.repository;

import org.example.notificationservice.model.entity.Notification;
import org.example.notificationservice.model.enums.StatutNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    //  est maintenant String (UUID)
    Page<Notification> findByClientIdOrderByCreatedAtDesc(String clientId, Pageable pageable);

    long countByClientIdAndStatut(String clientId, StatutNotification statut);

    List<Notification> findByStatutAndDateProgrammeeBefore(StatutNotification statut, LocalDateTime date);

    List<Notification> findByStatutAndTentativesLessThan(StatutNotification statut, Integer maxTentatives);

    long countByStatut(StatutNotification statut);

    @Query("SELECT n.type, n.statut, COUNT(n) FROM Notification n " +
           "WHERE n.createdAt >= :depuis GROUP BY n.type, n.statut")
    List<Object[]> countByTypeAndStatutSince(@Param("depuis") LocalDateTime depuis);
}
