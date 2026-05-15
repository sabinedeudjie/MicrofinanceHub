package org.example.clientservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientCreatedEvent {
    private String clientId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String createdBy;
}
