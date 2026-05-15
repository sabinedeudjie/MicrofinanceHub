package org.example.agencyservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgencyInfo {
    private String id;
    private String code;
    private String name;
    private String directorId;
    private String directorEmail;
    private String status;
}