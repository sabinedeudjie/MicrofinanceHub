package org.example.clientservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.clientservice.model.enums.DocumentType;
import org.example.clientservice.model.enums.VerificationStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {
    private String id;
    private String clientId;
    private DocumentType type;
    private String typeName;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private VerificationStatus verificationStatus;
    private String verifiedBy;
    private LocalDateTime verifiedAt;
    private LocalDateTime uploadedAt;
}
