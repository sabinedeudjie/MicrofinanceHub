package org.example.notificationservice.model.entity;

import org.example.notificationservice.model.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notif_client",  columnList = "client_id"),
    @Index(name = "idx_notif_statut",  columnList = "statut"),
    @Index(name = "idx_notif_date",    columnList = "date_envoi")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notif_seq")
    @SequenceGenerator(name = "notif_seq", sequenceName = "notif_sequence", allocationSize = 1)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "destinataire_email")
    private String destinataireEmail;

    @Column(name = "destinataire_telephone")
    private String destinataireTelephone;

    @Column(nullable = false)
    private String sujet;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeNotification type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CanalNotification canal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutNotification statut = StatutNotification.EN_ATTENTE;

    @Column(name = "priorite")
    @Builder.Default
    private Integer priorite = 5;

    @Column(name = "date_envoi")
    private LocalDateTime dateEnvoi;

    @Column(name = "date_programmee")
    private LocalDateTime dateProgrammee;

    @Column(name = "tentatives")
    @Builder.Default
    private Integer tentatives = 0;

    @Column(name = "erreur_message", columnDefinition = "TEXT")
    private String erreurMessage;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "reference_type")
    private String referenceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private NotificationTemplate template;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}