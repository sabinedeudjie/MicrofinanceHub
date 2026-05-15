package org.example.authservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// java.time.LocalDateTime;
import java.util.Set;

// com.fasterxml.jackson.annotation.JsonFormat;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String userRoleType;
    private Set<String> roles;
    private boolean enabled;
     private String createdAt;
    private String lastLoginAt;
    //  LocalDateTime createdAt;
    //  LocalDateTime lastLoginAt;

    // (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    //  LocalDateTime createdAt;
    
    // (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    //  LocalDateTime lastLoginAt;

}