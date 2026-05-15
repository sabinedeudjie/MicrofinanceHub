package org.example.notificationservice.model.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientEvent {
    private String clientId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String createdBy;
}
