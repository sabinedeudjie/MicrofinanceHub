package org.example.notificationservice.service;

import org.example.notificationservice.config.NotificationProperties;
import org.example.notificationservice.model.dto.NotificationRequest;
import org.example.notificationservice.model.dto.NotificationResponse;
import org.example.notificationservice.model.entity.Notification;
import org.example.notificationservice.model.enums.StatutNotification;
import org.example.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final NotificationProperties properties;

    // 
    //  PRINCIPAL
    // 
    @Transactional
    public NotificationResponse envoyerNotification(NotificationRequest request) {

        Notification notification = Notification.builder()
                .clientId(request.getClientId())
                .destinataireEmail(request.getEmail())
                .destinataireTelephone(request.getTelephone())
                .sujet(request.getSujet())
                .message(request.getMessage())
                .type(request.getType())
                .canal(request.getCanal())
                .priorite(request.getPriorite())
                .referenceId(request.getReferenceId())
                .referenceType(request.getReferenceType())
                .statut(
                        request.getDateProgrammee() != null
                                ? StatutNotification.PROGRAMMEE
                                : StatutNotification.EN_ATTENTE
                )
                .dateProgrammee(request.getDateProgrammee())
                .build();

        notification = notificationRepository.save(notification);

        log.info(
                "Notification créée [id={}] type={} canal={}",
                notification.getId(),
                notification.getType(),
                notification.getCanal()
        );

        if (notification.getStatut() == StatutNotification.PROGRAMMEE) {
            return toResponse(notification);
        }

        notification = router(notification);

        return toResponse(notification);
    }

    // 
    // 
    // 
    @Transactional
    public Notification router(Notification notification) {

        try {
            notification.setStatut(StatutNotification.EN_COURS);
            notificationRepository.save(notification);

            switch (notification.getCanal()) {

                case EMAIL -> envoyerEmail(notification);

                case SMS -> envoyerSms(notification);

                case IN_APP -> marquerEnvoyeeInApp(notification);

                case EMAIL_SMS -> {
                    envoyerEmail(notification);
                    envoyerSms(notification);
                }
            }

            notification.setStatut(StatutNotification.ENVOYEE);
            notification.setDateEnvoi(LocalDateTime.now());

            log.info(
                    "Notification [id={}] envoyée avec succès",
                    notification.getId()
            );

        } catch (Exception e) {
            gererEchec(notification, e);
        }

        return notificationRepository.save(notification);
    }

    // 
    // 
    // 
    private void envoyerEmail(Notification notification) {

        if (notification.getDestinataireEmail() == null
                || notification.getDestinataireEmail().isBlank()) {

            throw new IllegalArgumentException(
                    "Email destinataire manquant pour notification id=" + notification.getId()
            );
        }

        emailService.envoyer(
                notification.getDestinataireEmail(),
                notification.getSujet(),
                notification.getMessage()
        );
    }

    private void envoyerSms(Notification notification) {

        if (notification.getDestinataireTelephone() == null
                || notification.getDestinataireTelephone().isBlank()) {

            throw new IllegalArgumentException(
                    "Téléphone destinataire manquant pour notification id="
                            + notification.getId()
            );
        }

        smsService.envoyer(
                notification.getDestinataireTelephone(),
                notification.getMessage()
        );
    }

    private void marquerEnvoyeeInApp(Notification notification) {
        log.info(
                "Notification IN_APP [id={}] disponible dans l'application",
                notification.getId()
        );
    }

    // 
    //  ECHEC
    // 
    private void gererEchec(Notification notification, Exception e) {

        int tentatives = notification.getTentatives() == null
                ? 1
                : notification.getTentatives() + 1;

        notification.setTentatives(tentatives);
        notification.setErreurMessage(e.getMessage());

        if (tentatives >= properties.getRetryMax()) {

            notification.setStatut(
                    StatutNotification.ECHEC_DEFINITIF
            );

            log.error(
                    "Notification [id={}] échec définitif après {} tentative(s)",
                    notification.getId(),
                    tentatives
            );

        } else {

            notification.setStatut(StatutNotification.ECHEC);

            log.warn(
                    "Notification [id={}] échec tentative {}/{}",
                    notification.getId(),
                    tentatives,
                    properties.getRetryMax()
            );
        }
    }

    // 
    // 
    // 
    @Transactional
    public void reessayerNotificationsEnEchec() {

        List<Notification> notifications = notificationRepository
                .findByStatutAndTentativesLessThan(
                        StatutNotification.ECHEC,
                        properties.getRetryMax()
                );

        log.info(
                "Retry : {} notification(s) à retraiter",
                notifications.size()
        );

        notifications.forEach(this::router);
    }

    // 
    // 
    // 
    @Transactional
    public void envoyerNotificationsProgrammees() {

        List<Notification> notifications = notificationRepository
                .findByStatutAndDateProgrammeeBefore(
                        StatutNotification.PROGRAMMEE,
                        LocalDateTime.now()
                );

        log.info(
                "Scheduler : {} notification(s) programmée(s)",
                notifications.size()
        );

        notifications.forEach(this::router);
    }

    // 
    // 
    // 
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getHistoriqueClient(
            String clientId,
            Pageable pageable
    ) {
        return notificationRepository
                .findByClientIdOrderByCreatedAtDesc(clientId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public NotificationResponse getById(Long id) {

        Notification notification = notificationRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Notification non trouvée id=" + id
                        )
                );

        return toResponse(notification);
    }

    @Transactional(readOnly = true)
    public long countNonLues(String clientId) {
        return notificationRepository.countByClientIdAndStatut(
                clientId,
                StatutNotification.ENVOYEE
        );
    }

    // 
    //  COMME LUE
    // 
    @Transactional
    public void marquerCommeLue(Long id) {

        Notification notification = notificationRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Notification non trouvée id=" + id
                        )
                );

        notification.setStatut(StatutNotification.LUE);

        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getAll(Pageable pageable) {
        return notificationRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", notificationRepository.count());
        stats.put("envoyees", notificationRepository.countByStatut(StatutNotification.ENVOYEE));
        stats.put("enAttente", notificationRepository.countByStatut(StatutNotification.EN_ATTENTE)
                             + notificationRepository.countByStatut(StatutNotification.PROGRAMMEE));
        stats.put("echecs", notificationRepository.countByStatut(StatutNotification.ECHEC)
                          + notificationRepository.countByStatut(StatutNotification.ECHEC_DEFINITIF));
        return stats;
    }

    //
    //  DTO
    //
    private NotificationResponse toResponse(Notification n) {

        return NotificationResponse.builder()
                .id(n.getId())
                .clientId(n.getClientId())
                .sujet(n.getSujet())
                .message(n.getMessage())
                .type(n.getType())
                .canal(n.getCanal())
                .statut(n.getStatut())
                .priorite(n.getPriorite())
                .tentatives(n.getTentatives())
                .dateEnvoi(n.getDateEnvoi())
                .dateProgrammee(n.getDateProgrammee())
                .createdAt(n.getCreatedAt())
                .referenceId(n.getReferenceId())
                .referenceType(n.getReferenceType())
                .erreurMessage(n.getErreurMessage())
                .build();
    }
}