package org.example.notificationservice.model.dto;

import org.example.notificationservice.model.enums.CanalNotification;
import org.example.notificationservice.model.enums.TypeNotification;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {

    @NotNull(message = "Le clientId est obligatoire")
    private String clientId;

    @Email(message = "Email invalide")
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{9,15}$",
             message = "Numéro de téléphone invalide")
    private String telephone;

    @NotBlank(message = "Le sujet est obligatoire")
    @Size(max = 255)
    private String sujet;

    @NotBlank(message = "Le message est obligatoire")
    private String message;

    @NotNull(message = "Le type est obligatoire")
    private TypeNotification type;

    @NotNull(message = "Le canal est obligatoire")
    private CanalNotification canal;

    // .Default obligatoire quand on a une valeur par défaut avec @Builder
    @Builder.Default
    private Integer priorite = 5;

    private LocalDateTime dateProgrammee;

    private String referenceId;
    private String referenceType;
}