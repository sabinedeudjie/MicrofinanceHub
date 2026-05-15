package org.example.accountservice.exception;

import org.example.accountservice.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Intercepteur global des exceptions.
 * Transforme toutes les exceptions en réponses JSON claires
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Compte non trouvé → HTTP 404 */
    @ExceptionHandler(CompteNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCompteNotFound(CompteNotFoundException ex) {
        log.warn("non trouvé : {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /** Opération métier invalide → HTTP 400 */
    @ExceptionHandler(OperationInvalideException.class)
    public ResponseEntity<ApiResponse<Void>> handleOperationInvalide(OperationInvalideException ex) {
        log.warn("invalide : {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Erreurs de validation (@Valid) → HTTP 422
     * Renvoie le détail de chaque champ invalide.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> erreurs = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            erreurs.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("Erreurs de validation : {}", erreurs);

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Données invalides")
                        .data(erreurs)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    /** Erreur inattendue → HTTP 500 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("interne : {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Une erreur interne est survenue. Veuillez réessayer."));
    }
}
