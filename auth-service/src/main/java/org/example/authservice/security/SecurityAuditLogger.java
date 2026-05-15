package org.example.authservice.security;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SecurityAuditLogger {
    
    public void logLoginSuccess(String email, String ip) {
        log.info("- email: {}, ip: {}, time: {}", email, ip, LocalDateTime.now());
    }
    
    public void logLoginFailure(String email, String ip, String reason) {
        log.warn("- email: {}, ip: {}, reason: {}, time: {}", email, ip, reason, LocalDateTime.now());
    }
    
    public void logPasswordReset(String email, String method) {
        log.info("- email: {}, method: {}, time: {}", email, method, LocalDateTime.now());
    }
}
