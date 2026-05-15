package org.example.notificationservice.model.dto;

import org.example.notificationservice.model.enums.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;
    private String clientId;
    private String sujet;
    private String message;
    private TypeNotification type;
    private CanalNotification canal;
    private StatutNotification statut;
    private Integer priorite;
    private Integer tentatives;
    private LocalDateTime dateEnvoi;
    private LocalDateTime dateProgrammee;
    private LocalDateTime createdAt;
    private String referenceId;
    private String referenceType;
    private String erreurMessage;
}