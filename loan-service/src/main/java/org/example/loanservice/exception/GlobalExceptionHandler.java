package org.example.loanservice.exception;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ErrorResponse buildErrorResponse(HttpServletRequest request, int status, String error, String message) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .message(message)
                .path(request.getRequestURI())
                .build();
    }

    @ExceptionHandler(LoanAlreadyProcessedException.class)
    public ResponseEntity<ErrorResponse> handleLoanAlreadyProcessed(LoanAlreadyProcessedException ex, HttpServletRequest request) {
        log.warn("déjà traitée: {}", ex.getMessage());
        return new ResponseEntity<>(
            buildErrorResponse(request, HttpStatus.CONFLICT.value(), "Already Processed", ex.getMessage()),
            HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(LoanNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLoanNotFound(LoanNotFoundException ex, HttpServletRequest request) {
        log.error("non trouvé: {}", ex.getMessage());
        return new ResponseEntity<>(
            buildErrorResponse(request, HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage()),
            HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(LoanApplicationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLoanApplicationNotFound(LoanApplicationNotFoundException ex, HttpServletRequest request) {
        log.error("non trouvée: {}", ex.getMessage());
        return new ResponseEntity<>(
            buildErrorResponse(request, HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage()),
            HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(IneligibleClientException.class)
    public ResponseEntity<ErrorResponse> handleIneligibleClient(IneligibleClientException ex, HttpServletRequest request) {
        log.warn("non éligible: {}", ex.getMessage());
        return new ResponseEntity<>(
            buildErrorResponse(request, HttpStatus.BAD_REQUEST.value(), "Ineligible Client", ex.getMessage()),
            HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        ErrorResponse response = buildErrorResponse(request, HttpStatus.BAD_REQUEST.value(), "Validation Failed", "Erreur de validation");
        response.setValidationErrors(errors);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(FeignException ex, HttpServletRequest request) {
        log.error("Feign: {}", ex.getMessage());
        String message = ex.status() == 404 
            ? "Service client indisponible" 
            : "Erreur de communication entre services";
        return new ResponseEntity<>(
            buildErrorResponse(request, HttpStatus.SERVICE_UNAVAILABLE.value(), "Service Error", message),
            HttpStatus.SERVICE_UNAVAILABLE
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("interne: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(
            buildErrorResponse(request, HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Error", "Une erreur inattendue s'est produite"),
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    /**
     * Gère les RuntimeException (y compris les erreurs de vérification d'agence)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        String message = ex.getMessage();
        
        if (message != null && (
            message.contains("ne concerne pas votre agence") ||
            message.contains("n'êtes pas assigné") ||
            message.contains("n'est pas actif") ||
            message.contains("Client non éligible") ||
            message.contains("Demande déjà traitée") ||
            message.contains("Compte bancaire requis") ||
            message.contains("n'a pas d'agence assignée") ||
            message.contains("Impossible de vérifier l'agence") ||
            message.contains("Client non trouvé") ||
            message.contains("non trouvée") ||
            message.contains("non trouvé") ||
            message.contains("indisponible") ||
            message.contains("Communication error") ||
            message.contains("Erreur de communication")
        )) {
            log.warn("métier: {}", message);
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.BAD_REQUEST.value())
                    .error("Bad Request")
                    .message(message)
                    .path(request.getRequestURI())
                    .build();
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }

        if (message != null && message.contains("Rôle non autorisé")) {
            log.warn("autorisation refusée: {}", message);
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.FORBIDDEN.value())
                    .error("Forbidden")
                    .message(message)
                    .path(request.getRequestURI())
                    .build();
            return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
        }
        
        //  les autres RuntimeException, retourner un message générique
        log.error("interne: {}", message, ex);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Error")
                .message("Une erreur inattendue s'est produite")
                .path(request.getRequestURI())
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}