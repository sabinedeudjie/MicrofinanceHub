package org.example.authservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String userRoleType;
    private List<String> roles;
    private boolean enabled;
    private String createdAt;
    private String lastLoginAt;
    private String agencyId;
    private String agencyCode;
}