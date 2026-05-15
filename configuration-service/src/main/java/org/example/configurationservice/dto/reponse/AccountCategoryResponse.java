package org.example.configurationservice.dto.reponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountCategoryResponse {
    private String id;
    private String code;
    private String name;
    private String description;
    private String icon;
    private String color;
    private int displayOrder;
    private boolean active;
    private List<AccountTypeConfigurationResponse> accountTypes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}