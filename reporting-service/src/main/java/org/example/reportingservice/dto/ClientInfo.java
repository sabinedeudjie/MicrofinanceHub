package org.example.reportingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientInfo {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;
    private String clientType;
    private String status;
    private Integer creditScore;
    private String createdBy;
    private String createdAt;
    private String updatedAt;
    private String lastLoginAt;
}