package org.example.authservice.repository;

import org.example.authservice.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {
    
    Optional<UserSession> findBySessionToken(String sessionToken);
    
    List<UserSession> findByUserIdAndIsActiveTrue(String userId);
    
    @Transactional
    @Modifying
    @Query("UPDATE UserSession us SET us.isActive = false WHERE us.user.id = :userId")
    void deactivateAllSessionsByUserId(@Param("userId") String userId);
    
    @Transactional
    @Modifying
    @Query("UPDATE UserSession us SET us.isActive = false WHERE us.sessionToken = :sessionToken")
    void deactivateSessionByToken(@Param("sessionToken") String sessionToken);
    
    @Transactional
    @Modifying
    @Query("DELETE FROM UserSession us WHERE us.expiresAt < :now")
    void deleteExpiredSessions(@Param("now") LocalDateTime now);

    @Transactional
    @Modifying
    @Query("DELETE FROM UserSession us WHERE us.user.id = :userId")
    void deleteByUserId(@Param("userId") String userId);
}