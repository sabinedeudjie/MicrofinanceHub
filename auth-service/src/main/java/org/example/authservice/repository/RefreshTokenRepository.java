package org.example.authservice.repository;

import org.example.authservice.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    
    Optional<RefreshToken> findByToken(String token);
    
    @Transactional
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId")
    void deleteByUserId(@Param("userId") String userId);
}

//  org.example.authservice.repository;

//  org.example.authservice.model.RefreshToken;
//  org.springframework.data.jpa.repository.JpaRepository;
//  org.springframework.stereotype.Repository;

//  java.util.Optional;

// 
//  interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    
//      findByToken(String token);
    
//      deleteByUserId(String userId);
// 