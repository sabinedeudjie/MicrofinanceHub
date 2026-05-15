package org.example.agencyservice.dto.response;

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
public class AgencyResponse {
    private String id;
    private String code;
    private String name;
    private String address;
    private String city;
    private String phoneNumber;
    private String email;
    private String directorId;
    private String directorEmail;
    private String directorName;
    private String region;
    private String status;
    private Long agentsCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long totalClients;      //  rempli par Account Service
    private Long totalAccounts;     //  rempli par Account Service
    private Long totalLoans;        //  rempli par Loan Service
    private BigDecimal totalOutstanding;  //  rempli par Loan Service
    private Double monthlyRepaymentRate;  //  rempli par Repayment Service
}