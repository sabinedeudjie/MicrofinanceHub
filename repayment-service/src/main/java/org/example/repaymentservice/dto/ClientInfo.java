package org.example.repaymentservice.dto;

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
    private String userRoleType;
    private boolean enabled;
}