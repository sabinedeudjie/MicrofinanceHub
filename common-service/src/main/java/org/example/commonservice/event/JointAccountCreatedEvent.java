package org.example.commonservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JointAccountCreatedEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String eventId;
    private String jointAccountId;
    private String accountNumber;
    private String accountName;
    private String accountType;
    private BigDecimal initialBalance;
    private String currency;
    private List<String> clientIds;
    private List<ClientInfo> clients;
    private String createdBy;
    private LocalDateTime createdAt;
    private boolean requiresBothSignatures;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientInfo implements Serializable {
        private String clientId;
        private String email;
        private String firstName;
        private String lastName;
        private String phoneNumber;
    }
}