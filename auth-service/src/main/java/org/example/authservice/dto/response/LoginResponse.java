package org.example.authservice.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    
    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("refresh_token")
    private String refreshToken;
    
    @JsonProperty("session_token")
    private String sessionToken;
    
    @JsonProperty("token_type")
    @Builder.Default
    private String tokenType = "Bearer";
    
    private String email;

    private String role;

    private String firstName;

    private String lastName;

    @JsonProperty("agency_id")
    private String agencyId;

    @JsonProperty("expires_in")
    private Long expiresIn;
    
    @JsonProperty("has_security_question")
    private boolean hasSecurityQuestion;

    private String status;
}

//  org.example.authservice.dto.response;

//  com.fasterxml.jackson.annotation.JsonProperty;
//  lombok.AllArgsConstructor;
//  lombok.Builder;
//  lombok.Data;
//  lombok.NoArgsConstructor;

// 
// 
// 
// 
//  class LoginResponse {
    
//     ("access_token")
//      String accessToken;
    
//     ("token_type")
//     .Default
//      String tokenType = "Bearer";
    
//      String email;
    
//      String role;
    
//      String firstName;
    
//      String lastName;
    
//     ("expires_in")
//      Long expiresIn;
// 