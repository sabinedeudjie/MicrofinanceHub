package org.example.authservice.repository;

import org.example.authservice.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {
    
    Optional<PasswordResetToken> findByToken(String token);
    
    @Modifying
    @Transactional
    void deleteByUserId(String userId);
    
    //  alternative avec @Query explicite
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken p WHERE p.user.id = :userId")
    void deleteByUserIdQuery(@Param("userId") String userId);
}

//  org.example.authservice.repository;

//  org.example.authservice.model.PasswordResetToken;
//  org.springframework.data.jpa.repository.JpaRepository;
//  org.springframework.stereotype.Repository;

//  java.util.Optional;

// 
//  interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {
    
//      findByToken(String token);
    
//      deleteByUserId(String userId);
// 