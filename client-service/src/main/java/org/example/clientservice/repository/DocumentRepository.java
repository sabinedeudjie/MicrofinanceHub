package org.example.clientservice.repository;

import org.example.clientservice.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, String> {

    @Query("SELECT d FROM Document d WHERE d.client.id = :clientId ORDER BY d.uploadedAt DESC")
    List<Document> findByClientId(@Param("clientId") String clientId);
}
