package org.example.clientservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // 
    //  MÉTIER (4xx)
    // 
    
    @ExceptionHandler(ClientNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleClientNotFound(
            ClientNotFoundException ex, 
            HttpServletRequest request) {
        
        log.warn("non trouvé: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(
            DuplicateEmailException ex, 
            HttpServletRequest request) {
        
        log.warn("dupliqué: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(DuplicatePhoneException.class)
    public ResponseEntity<ErrorResponse> handleDuplicatePhone(
            DuplicatePhoneException ex, 
            HttpServletRequest request) {
        
        log.warn("dupliqué: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccess(
            UnauthorizedAccessException ex, 
            HttpServletRequest request) {
        
        log.warn("non autorisé: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
    
    @ExceptionHandler(InvalidClientDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidClientData(
            InvalidClientDataException ex, 
            HttpServletRequest request) {
        
        log.warn("client invalides: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    // 
    //  DE VALIDATION
    // 
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage(),
                        (existing, replacement) -> existing
                ));
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Erreur de validation des données")
                .path(request.getRequestURI())
                .validationErrors(errors)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    // 
    //  DE SÉCURITÉ
    // 
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {
        
        log.warn("refusé à {} depuis {}", request.getRequestURI(), request.getRemoteAddr());
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message("Vous n'avez pas les droits nécessaires pour accéder à cette ressource")
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {
        
        log.warn("échouée pour {}", request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message("Authentification requise")
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
    
    // 
    //  TECHNIQUES
    // 
    
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        
        String message = String.format("Le paramètre '%s' doit être de type %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(message)
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("Paramètre manquant: " + ex.getParameterName())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("Corps de la requête invalide ou mal formé")
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    // 
    //  BASE DE DONNÉES
    // 
    
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {
        
        log.warn("de contrainte d'intégrité: {}", ex.getMessage());
        
        String message = "Violation de contrainte de données";
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("uk_email")) {
                message = "Cet email est déjà utilisé";
            } else if (ex.getMessage().contains("uk_phone")) {
                message = "Ce numéro de téléphone est déjà utilisé";
            } else if (ex.getMessage().contains("not-null")) {
                message = "Un champ obligatoire est manquant";
            }
        }
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Data Conflict")
                .message(message)
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseError(
            DataAccessException ex,
            HttpServletRequest request) {
        
        log.error("base de données: {}", ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Database Error")
                .message("Erreur lors de l'accès aux données")
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    // 
    //  REDIS (RATE LIMITING)
    // 
    
    @ExceptionHandler(RedisConnectionFailureException.class)
    public ResponseEntity<ErrorResponse> handleRedisConnectionFailure(
            RedisConnectionFailureException ex,
            HttpServletRequest request) {
        
        log.error("de connexion Redis: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable")
                .message("Service de rate limiting temporairement indisponible")
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }
    
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message("Ressource non trouvée: " + request.getRequestURI())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    //
    //  GÉNÉRIQUES (500)
    //

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        
        log.error("interne: {}", ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Une erreur inattendue s'est produite")
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}


//  org.example.clientservice.exception;

//  jakarta.servlet.http.HttpServletRequest;
//  lombok.extern.slf4j.Slf4j;
//  org.springframework.http.HttpStatus;
//  org.springframework.http.ResponseEntity;
//  org.springframework.http.converter.HttpMessageNotReadableException;
//  org.springframework.security.access.AccessDeniedException;
//  org.springframework.security.core.AuthenticationException;
//  org.springframework.validation.FieldError;
//  org.springframework.web.bind.MethodArgumentNotValidException;
//  org.springframework.web.bind.MissingServletRequestParameterException;
//  org.springframework.web.bind.annotation.ExceptionHandler;
//  org.springframework.web.bind.annotation.RestControllerAdvice;
//  org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

//  java.time.LocalDateTime;
//  java.util.Map;
//  java.util.stream.Collectors;

// 
// 
//  class GlobalExceptionHandler {
    
//     // 
//     //  MÉTIER (4xx)
//     // 
    
//     (ClientNotFoundException.class)
//      ResponseEntity<ErrorResponse> handleClientNotFound(
//              ex, 
//              request) {
        
//         .warn("non trouvé: {}", ex.getMessage());
        
//          error = ErrorResponse.builder()
//                 .(LocalDateTime.now())
//                 .(HttpStatus.NOT_FOUND.value())
//                 .("Not Found")
//                 .(ex.getMessage())
//                 .(request.getRequestURI())
//                 .();
        
//          ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
//     
    
//     (DuplicateEmailException.class)
//      ResponseEntity<ErrorResponse> handleDuplicateEmail(
//              ex, 
//              request) {
        
//         .warn("dupliqué: {}", ex.getMessage());
        
//          error = ErrorResponse.builder()
//                 .(LocalDateTime.now())
//                 .(HttpStatus.CONFLICT.value())
//                 .("Conflict")
//                 .(ex.getMessage())
//                 .(request.getRequestURI())
//                 .();
        
//          ResponseEntity.status(HttpStatus.CONFLICT).body(error);
//     
    
//     (DuplicatePhoneException.class)
//      ResponseEntity<ErrorResponse> handleDuplicatePhone(
//              ex, 
//              request) {
        
//         .warn("dupliqué: {}", ex.getMessage());
        
//          error = ErrorResponse.builder()
//                 .(LocalDateTime.now())
//                 .(HttpStatus.CONFLICT.value())
//                 .("Conflict")
//                 .(ex.getMessage())
//                 .(request.getRequestURI())
//                 .();
        
//          ResponseEntity.status(HttpStatus.CONFLICT).body(error);
//     
    
//     (UnauthorizedAccessException.class)
//      ResponseEntity<ErrorResponse> handleUnauthorizedAccess(
//              ex, 
//              request) {
        
//         .warn("non autorisé: {}", ex.getMessage());
        
//          error = ErrorResponse.builder()
//                 .(LocalDateTime.now())
//                 .(HttpStatus.FORBIDDEN.value())
//                 .("Forbidden")
//                 .(ex.getMessage())
//                 .(request.getRequestURI())
//                 .();
        
//          ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
//     
    
//     (InvalidClientDataException.class)
//      ResponseEntity<ErrorResponse> handleInvalidClientData(
//              ex, 
//              request) {
        
//         .warn("client invalides: {}", ex.getMessage());
        
//          error = ErrorResponse.builder()
//                 .(LocalDateTime.now())
//                 .(HttpStatus.BAD_REQUEST.value())
//                 .("Bad Request")
//                 .(ex.getMessage())
//                 .(request.getRequestURI())
//                 .();
        
//          ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
//     
    
//     // 
//     //  DE VALIDATION
//     // 
    
//     (MethodArgumentNotValidException.class)
//      ResponseEntity<ErrorResponse> handleValidationExceptions(
//              ex,
//              request) {
        
//         , String> errors = ex.getBindingResult()
//                 .()
//                 .()
//                 .(Collectors.toMap(
//                         ,
//                          -> error.getDefaultMessage(),
//                         (, replacement) -> existing
//                 ))
        
//          error = ErrorResponse.builder()
//                 .(LocalDateTime.now())
//                 .(HttpStatus.BAD_REQUEST.value())
//                 .("Validation Failed")
//                 .("Erreur de validation des données")
//                 .(request.getRequestURI())
//                 .(errors)
//                 .();
        
//          ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
//     
    
//     // 
//     //  DE SÉCURITÉ
//     // 
    
//     (AccessDeniedException.class)
//      ResponseEntity<ErrorResponse> handleAccessDenied(
//              ex,
//              request) {
        
//         .warn("refusé à {} depuis {}", request.getRequestURI(), request.getRemoteAddr());
        
//          error = ErrorResponse.builder()
//                 .(LocalDateTime.now())
//                 .(HttpStatus.FORBIDDEN.value())
//                 .("Forbidden")
//                 .("Vous n'avez pas les droits nécessaires pour accéder à cette ressource")
//                 .(request.getRequestURI())
//                 .();
        
//          ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
//     
    
//     (AuthenticationException.class)
//      ResponseEntity<ErrorResponse> handleAuthenticationException(
//              ex,
//              request) {
        
//         .warn("échouée pour {}", request.getRequestURI());
        
//          error = ErrorResponse.builder()
//                 .(LocalDateTime.now())
//                 .(HttpStatus.UNAUTHORIZED.value())
//                 .("Unauthorized")
//                 .("Authentification requise")
//                 .(request.getRequestURI())
//                 .();
        
//          ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
//     
    
//     // 
//     //  TECHNIQUES
//     // 
    
//     (MethodArgumentTypeMismatchException.class)
//      ResponseEntity<ErrorResponse> handleTypeMismatch(
//              ex,
//              request) {
        
//          message = String.format("Le paramètre '%s' doit être de type %s",
//                 .getName(),
//                 .getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        
//          error = ErrorResponse.builder()
//                 .(LocalDateTime.now())
//                 .(HttpStatus.BAD_REQUEST.value())
//                 .("Bad Request")
//                 .(message)
//                 .(request.getRequestURI())
//                 .();
        
//          ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
//     
    
//     (MissingServletRequestParameterException.class)
//      ResponseEntity<ErrorResponse> handleMissingParameter(
//              ex,
//              request) {
        
//          error = ErrorResponse.builder()
//                 .(LocalDateTime.now())
//                 .(HttpStatus.BAD_REQUEST.value())
//                 .("Bad Request")
//                 .("Paramètre manquant: " + ex.getParameterName())
//                 .(request.getRequestURI())
//                 .();
        
//          ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
//     
    
//     (HttpMessageNotReadableException.class)
//      ResponseEntity<ErrorResponse> handleMessageNotReadable(
//              ex,
//              request) {
        
//          error = ErrorResponse.builder()
//                 .(LocalDateTime.now())
//                 .(HttpStatus.BAD_REQUEST.value())
//                 .("Bad Request")
//                 .("Corps de la requête invalide ou mal formé")
//                 .(request.getRequestURI())
//                 .();
        
//          ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
//     
    
//     // 
//     //  GÉNÉRIQUES (500)
//     // 
    
//     (Exception.class)
//      ResponseEntity<ErrorResponse> handleGenericException(
//              ex,
//              request) {
        
//         .error("interne: {}", ex.getMessage(), ex);
        
//          error = ErrorResponse.builder()
//                 .(LocalDateTime.now())
//                 .(HttpStatus.INTERNAL_SERVER_ERROR.value())
//                 .("Internal Server Error")
//                 .("Une erreur inattendue s'est produite")
//                 .(request.getRequestURI())
//                 .();
        
//          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
//     
// 