package org.example.agencyservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(AgencyNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAgencyNotFound(AgencyNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Agency Not Found", ex.getMessage());
    }
    
    @ExceptionHandler(AgentAlreadyAssignedException.class)
    public ResponseEntity<Map<String, Object>> handleAgentAlreadyAssigned(AgentAlreadyAssignedException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Agent Already Assigned", ex.getMessage());
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", ex.getMessage());
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Error", "Une erreur inattendue s'est produite");
    }
    
    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String error, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        response.put("error", error);
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
    }
}