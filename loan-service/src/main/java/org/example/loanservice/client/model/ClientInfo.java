package org.example.loanservice.client.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private String userType;  // , AGENT, ADMIN
    private boolean enabled;
    private Integer creditScore;
    private BigDecimal monthlyIncome;
    private String employmentStatus;
    private LocalDateTime createdAt;
}