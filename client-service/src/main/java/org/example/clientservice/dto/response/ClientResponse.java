package org.example.clientservice.dto.response;

import org.example.clientservice.model.enums.ClientStatus;
import org.example.clientservice.model.enums.ClientType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientResponse {
    
    private String id;
    private String email;
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private String address;
    private LocalDateTime birthDate;
    private ClientType clientType;
    private ClientStatus status;
    private Integer creditScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    private String agencyId;
    private String createdBy;
}