package org.example.authservice.service;

import org.example.authservice.model.User;
import org.example.authservice.model.UserSession;
// org.example.authservice.repository.UserRepository;
import org.example.authservice.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {
    
    private final UserSessionRepository sessionRepository;
    // final UserRepository userRepository;
    
    private static final int SESSION_DURATION_HOURS = 24;
    
    @Transactional
    public UserSession createSession(User user, String deviceInfo, String ipAddress) {
        String sessionToken = UUID.randomUUID().toString();
        
        UserSession session = UserSession.builder()
                .user(user)
                .sessionToken(sessionToken)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .expiresAt(LocalDateTime.now().plusHours(SESSION_DURATION_HOURS))
                .build();
        
        return sessionRepository.save(session);
    }
    
    @Transactional
    public void updateSessionActivity(String sessionToken) {
        sessionRepository.findBySessionToken(sessionToken).ifPresent(session -> {
            session.setLastActivity(LocalDateTime.now());
            sessionRepository.save(session);
        });
    }
    
    @Transactional
    public void logout(String sessionToken) {
        sessionRepository.deactivateSessionByToken(sessionToken);
        log.info("déconnectée: {}", sessionToken);
    }
    
    @Transactional
    public void logoutAllDevices(String userId) {
        sessionRepository.deactivateAllSessionsByUserId(userId);
        log.info("les sessions de l'utilisateur {} ont été déconnectées", userId);
    }
    
    public boolean isSessionValid(String sessionToken) {
        return sessionRepository.findBySessionToken(sessionToken)
                .map(session -> session.isActive() && session.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElse(false);
    }
    
    @Scheduled(cron = "0 0 * * * *") //  les heures
    @Transactional
    public void cleanupExpiredSessions() {
        sessionRepository.deleteExpiredSessions(LocalDateTime.now());
        log.info("expirées nettoyées");
    }


    public List<UserSession> getActiveSessions(String userId) {
        return sessionRepository.findByUserIdAndIsActiveTrue(userId);
    }
    
    public boolean revokeSession(String sessionId, String userId) {
        return sessionRepository.findById(sessionId)
                .map(session -> {
                    if (session.getUser().getId().equals(userId)) {
                        session.setActive(false);
                        sessionRepository.save(session);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }


}