package org.example.authservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        Map<String, Object> errorResponse = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Bad Request");
        errorResponse.put("message", "Validation failed");
        errorResponse.put("errors", errors);
        errorResponse.put("path", request.getServletPath());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentialsException(
            BadCredentialsException ex, 
            HttpServletRequest request) {
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", HttpStatus.UNAUTHORIZED.value());
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("message", "Email ou mot de passe incorrect. Veuillez réessayer.");
        errorResponse.put("path", request.getServletPath());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex, 
            HttpServletRequest request) {
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Bad Request");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("path", request.getServletPath());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}

//  org.example.authservice.exception;

//  org.springframework.http.HttpStatus;
//  org.springframework.http.ResponseEntity;
//  org.springframework.security.authentication.BadCredentialsException;
//  org.springframework.web.bind.annotation.ExceptionHandler;
//  org.springframework.web.bind.annotation.RestControllerAdvice;

//  jakarta.servlet.http.HttpServletRequest;
//  java.time.LocalDateTime;
//  java.util.HashMap;
//  java.util.Map;

// 
//  class GlobalExceptionHandler {

//     (BadCredentialsException.class)
//      ResponseEntity<Map<String, Object>> handleBadCredentialsException(
//              ex, 
//              request) {
        
//         , Object> errorResponse = new HashMap<>();
//         .put("timestamp", LocalDateTime.now().toString());
//         .put("status", HttpStatus.UNAUTHORIZED.value());
//         .put("error", "Unauthorized");
//         .put("message", "Bad credentials");
//         .put("path", request.getServletPath());
        
//          new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
//     

//     (RuntimeException.class)
//      ResponseEntity<Map<String, Object>> handleRuntimeException(
//              ex, 
//              request) {
        
//         , Object> errorResponse = new HashMap<>();
//         .put("timestamp", LocalDateTime.now().toString());
//         .put("status", HttpStatus.BAD_REQUEST.value());
//         .put("error", "Bad Request");
//         .put("message", ex.getMessage());
//         .put("path", request.getServletPath());
        
//          new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
//     
// 