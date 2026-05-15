package org.example.clientservice.dto.request;

import org.example.clientservice.model.enums.ClientStatus;
import org.example.clientservice.model.enums.ClientType;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class ClientSearchRequest {
    
    private String email;
    
    private String phone;
    
    private ClientStatus status;
    
    private ClientType clientType;
    
    private Integer minScore;
    
    private Integer maxScore;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdAfter;
    
    private String search;
    
    private Integer page = 0;
    
    private Integer size = 20;
    
    private String sortBy = "createdAt";
    
    private String sortDirection = "DESC";
}