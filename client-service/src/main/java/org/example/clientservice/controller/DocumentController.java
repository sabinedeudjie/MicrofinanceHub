package org.example.clientservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.dto.response.DocumentResponse;
import org.example.clientservice.model.enums.DocumentType;
import org.example.clientservice.model.enums.VerificationStatus;
import org.example.clientservice.service.DocumentService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/api/clients/{clientId}/documents")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @PathVariable String clientId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") DocumentType type) throws IOException {
        log.info("Upload document {} pour client {}", type, clientId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.uploadDocument(clientId, file, type));
    }

    @GetMapping("/api/clients/{clientId}/documents")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<List<DocumentResponse>> getClientDocuments(@PathVariable String clientId) {
        return ResponseEntity.ok(documentService.getClientDocuments(clientId));
    }

    @PatchMapping("/api/documents/{docId}/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTEUR_AGENCE')")
    public ResponseEntity<DocumentResponse> verifyDocument(
            @PathVariable String docId,
            @RequestParam VerificationStatus status,
            Authentication authentication) {
        log.info("Vérification document {} → {} par {}", docId, status, authentication.getName());
        return ResponseEntity.ok(documentService.verifyDocument(docId, status, authentication.getName()));
    }

    @GetMapping("/api/documents/files/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) throws IOException {
        Resource resource = documentService.loadFileAsResource(filename);
        String contentType = Files.probeContentType(Paths.get(filename));
        if (contentType == null) contentType = "application/octet-stream";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
