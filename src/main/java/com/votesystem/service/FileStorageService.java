package com.votesystem.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final String UPLOAD_DIR = "images";

    public FileStorageService() {
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Stores an uploaded multipart file securely and returns the stored filename.
     */
    public String storeFile(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            String original = file.getOriginalFilename();
            String extension = "";
            if (original != null && original.contains(".")) {
                extension = original.substring(original.lastIndexOf(".")).toLowerCase();
            } else {
                extension = ".jpg";
            }

            // Security: Only permit safe image extensions
            if (!extension.matches("\\.(jpg|jpeg|png|gif|webp|svg)")) {
                log.warn("Rejected unsafe file extension: {}", extension);
                return null;
            }

            String filename = prefix + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
            Path targetLocation = Paths.get(UPLOAD_DIR).resolve(filename);

            try (InputStream is = file.getInputStream()) {
                Files.copy(is, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Stored uploaded file: {}", filename);
            return filename;
        } catch (Exception e) {
            log.error("Failed to store file: {}", e.getMessage());
            return null;
        }
    }
}
