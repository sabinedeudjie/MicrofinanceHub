package org.example.clientservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.dto.response.DocumentResponse;
import org.example.clientservice.exception.ClientNotFoundException;
import org.example.clientservice.model.Client;
import org.example.clientservice.model.Document;
import org.example.clientservice.model.enums.DocumentType;
import org.example.clientservice.model.enums.VerificationStatus;
import org.example.clientservice.repository.ClientRepository;
import org.example.clientservice.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ClientRepository   clientRepository;
    private final CloudinaryService cloudinaryService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.service.base-url:http://localhost:8081}")
    private String serviceBaseUrl;

    @Transactional
    public DocumentResponse uploadDocument(String clientId, MultipartFile file, DocumentType type) throws IOException {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Client non trouvé: " + clientId));

        // Buffer les bytes en mémoire pour pouvoir faire un fallback si Drive échoue
        byte[] fileBytes = file.getInputStream().readAllBytes();

        String fileUrl = null;
        if (cloudinaryService.isConfigured()) {
            try {
                fileUrl = cloudinaryService.uploadFile(
                        fileBytes,
                        file.getOriginalFilename(),
                        clientId);
            } catch (Exception e) {
                log.warn("Échec upload Cloudinary pour client {}, fallback stockage local: {}", clientId, e.getMessage());
            }
        }
        if (fileUrl == null) {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String ext = getExtension(file.getOriginalFilename());
            String uniqueName = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
            Files.copy(new ByteArrayInputStream(fileBytes), uploadPath.resolve(uniqueName), StandardCopyOption.REPLACE_EXISTING);
            fileUrl = serviceBaseUrl + "/api/documents/files/" + uniqueName;
        }

        Document doc = Document.builder()
                .client(client)
                .type(type)
                .fileName(file.getOriginalFilename())
                .fileUrl(fileUrl)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        doc = documentRepository.save(doc);
        log.info("Document {} uploadé pour client {}", type, clientId);
        return toResponse(doc);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getClientDocuments(String clientId) {
        return documentRepository.findByClientId(clientId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public DocumentResponse verifyDocument(String docId, VerificationStatus status, String verifiedBy) {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("Document non trouvé: " + docId));
        doc.setVerificationStatus(status);
        doc.setVerifiedBy(verifiedBy);
        doc.setVerifiedAt(LocalDateTime.now());
        return toResponse(documentRepository.save(doc));
    }

    public Resource loadFileAsResource(String filename) throws IOException {
        Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
        Resource resource = new UrlResource(filePath.toUri());
        if (resource.exists() && resource.isReadable()) return resource;
        throw new RuntimeException("Fichier introuvable: " + filename);
    }

    private DocumentResponse toResponse(Document d) {
        return DocumentResponse.builder()
                .id(d.getId())
                .clientId(d.getClient().getId())
                .type(d.getType())
                .typeName(labelType(d.getType()))
                .fileName(d.getFileName())
                .fileUrl(d.getFileUrl())
                .fileType(d.getFileType())
                .fileSize(d.getFileSize())
                .verificationStatus(d.getVerificationStatus())
                .verifiedBy(d.getVerifiedBy())
                .verifiedAt(d.getVerifiedAt())
                .uploadedAt(d.getUploadedAt())
                .build();
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private String labelType(DocumentType t) {
        return switch (t) {
            case ID_CARD              -> "Carte d'identité";
            case PASSPORT             -> "Passeport";
            case DRIVER_LICENSE       -> "Permis de conduire";
            case PROOF_OF_ADDRESS     -> "Justificatif de domicile";
            case BUSINESS_REGISTRATION -> "Registre de commerce";
            case TAX_IDENTIFICATION   -> "Numéro fiscal (TIN)";
            case BANK_STATEMENT       -> "Relevé bancaire";
            case PHOTO                -> "Photo d'identité";
        };
    }
}
