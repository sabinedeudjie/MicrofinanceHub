package org.example.notificationservice.model.entity;

import org.example.notificationservice.model.enums.*;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;         //  "PRET_APPROUVE_EMAIL"

    @Column(nullable = false)
    private String sujet;       //  "Votre prêt a été approuvé"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;     //  avec variables {{nom}}, {{montant}}

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeNotification typeNotification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CanalNotification canal;

    @Column(name = "actif")
    @Builder.Default
    private Boolean actif = true;

    @Column(name = "fichier_html")
    private String fichierHtml;  //  vers template Thymeleaf
}