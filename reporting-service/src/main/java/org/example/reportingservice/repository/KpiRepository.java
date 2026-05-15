package org.example.reportingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.example.reportingservice.model.Kpi;

import java.util.List;

@Repository
public interface KpiRepository extends JpaRepository<Kpi, String> {
    
    List<Kpi> findByCategory(String category);
    
    List<Kpi> findByCategoryOrderByCalculatedAtDesc(String category);
}