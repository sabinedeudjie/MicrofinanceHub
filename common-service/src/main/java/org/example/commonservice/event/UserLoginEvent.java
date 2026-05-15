package org.example.commonservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String email;
    private String userId;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime timestamp;
    private String sessionId;
}