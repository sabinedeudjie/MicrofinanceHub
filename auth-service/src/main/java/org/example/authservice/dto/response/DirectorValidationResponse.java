package org.example.authservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectorValidationResponse {
    private boolean exists;
    private String id;
    private String email;
    private String fullName;
    private String currentRole;
    private String currentAgencyId;
    private String currentAgencyCode;
    private boolean canBeAssigned;
    private String message;
}