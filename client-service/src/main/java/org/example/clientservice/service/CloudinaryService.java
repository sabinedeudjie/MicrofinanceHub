package org.example.clientservice.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
public class CloudinaryService {

    @Value("${cloudinary.url:}")
    private String cloudinaryUrl;

    private Cloudinary cloudinary;

    @PostConstruct
    public void init() {
        if (cloudinaryUrl == null || cloudinaryUrl.isBlank()) {
            log.info("Cloudinary non configuré — le stockage local sera utilisé");
            return;
        }
        try {
            cloudinary = new Cloudinary(cloudinaryUrl);
            log.info("Cloudinary initialisé avec succès");
        } catch (Exception e) {
            log.warn("Impossible d'initialiser Cloudinary: {}", e.getMessage());
        }
    }

    public boolean isConfigured() {
        return cloudinary != null;
    }

    /**
     * Uploads a file to Cloudinary.
     * Returns the public secure URL.
     */
    public String uploadFile(byte[] fileBytes, String filename, String folder) throws IOException {
        String safeFilename = filename;
        if (safeFilename != null && safeFilename.contains(".")) {
            // Cloudinary requires public_id without extension if resource_type=auto, or we can just pass original filename
            safeFilename = safeFilename.substring(0, safeFilename.lastIndexOf('.'));
        }
        safeFilename = safeFilename != null ? safeFilename.replaceAll("[^a-zA-Z0-9_.-]", "_") : "document";

        Map params = ObjectUtils.asMap(
                "folder", "microfinance/" + folder,
                "resource_type", "auto",
                "public_id", safeFilename
        );

        Map uploadResult = cloudinary.uploader().upload(fileBytes, params);
        
        log.info("Document '{}' uploadé sur Cloudinary dans le dossier {}", filename, folder);
        return uploadResult.get("secure_url").toString();
    }
}
