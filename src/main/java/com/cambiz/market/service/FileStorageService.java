package com.cambiz.market.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {
    
    private final Path uploadDir;
    
    public FileStorageService(@Value("${app.upload.dir:uploads/products}") String uploadDirPath) {
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadDirPath, e);
        }
    }
    
    public String storeFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String newFilename = UUID.randomUUID().toString() + extension;
        
        try {
            Path targetLocation = this.uploadDir.resolve(newFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/products/" + newFilename;
        } catch (IOException e) {
            throw new RuntimeException("Could not store file " + newFilename, e);
        }
    }
    
    public void deleteFile(String fileUrl) {
        if (fileUrl != null && fileUrl.startsWith("/uploads/")) {
            try {
                Path filePath = this.uploadDir.resolve(
                    fileUrl.replace("/uploads/products/", "")
                );
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                System.err.println("Could not delete file: " + fileUrl + " - " + e.getMessage());
            }
        }
    }
}