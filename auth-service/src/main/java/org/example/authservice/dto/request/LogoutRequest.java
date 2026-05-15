package org.example.authservice.dto.request;

import lombok.Data;

@Data
public class LogoutRequest {
    private String refreshToken;
    private Boolean allDevices;
}