package org.example.repaymentservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import org.example.repaymentservice.dto.response.ErrorResponse;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    //  des exceptions métier
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, WebRequest request) {
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .code(ex.getCode())
            .message(ex.getMessage())
            .path(getPath(request))
            .build();
        
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(feign.FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(
            feign.FeignException ex, WebRequest request) {
        
        String body = ex.contentUTF8();
        String cleanMessage = extractMessageFromJson(body);
        
        if (cleanMessage == null || cleanMessage.isBlank()) {
            cleanMessage = ex.getMessage();
        }

        String code = "EXTERNAL_SERVICE_ERROR";
        int status = ex.status() > 0 ? ex.status() : 500;

        if (body != null) {
            if (body.contains("SOLDE_INSUFFISANT") || body.toLowerCase().contains("solde insuffisant")) {
                code = "INSUFFICIENT_FUNDS";
                if (cleanMessage.contains("SOLDE_INSUFFISANT")) {
                    cleanMessage = cleanMessage.replace("SOLDE_INSUFFISANT", "").trim();
                    if (cleanMessage.startsWith(":")) cleanMessage = cleanMessage.substring(1).trim();
                }
            } else if (body.contains("DEMO_LIMIT") || body.contains("ER201")) {
                code = "LIMIT_EXCEEDED";
                cleanMessage = "Limite du système de démonstration (max 25 XAF). Veuillez essayer un montant plus petit.";
            }
        }

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(status)
            .code(code)
            .message(cleanMessage)
            .path(getPath(request))
            .build();
        
        return new ResponseEntity<>(error, HttpStatus.valueOf(status == 422 ? 400 : status));
    }

    private String extractMessageFromJson(String json) {
        if (json == null || !json.contains("\"message\"")) return null;
        try {
            int start = json.indexOf("\"message\":\"") + 11;
            if (start < 11) return null;
            int end = json.indexOf("\"", start);
            if (end == -1) return null;
            String msg = json.substring(start, end);
            // Dé-échappement basique si nécessaire
            return msg.replace("\\\"", "\"");
        } catch (Exception e) {
            return null;
        }
    }
    
    //  des exceptions techniques
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .code("TECHNICAL_ERROR")
            .message(ex.getMessage() != null ? ex.getMessage() : "Une erreur technique s'est produite")
            .path(getPath(request))
            .build();
        
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
