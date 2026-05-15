package org.example.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SecurityQuestionRequest {
    
    @NotBlank(message = "La question de sécurité est requise")
    private String question;
    
    @NotBlank(message = "La réponse est requise")
    private String answer;
}