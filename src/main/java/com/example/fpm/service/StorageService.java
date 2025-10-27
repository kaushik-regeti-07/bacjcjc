package com.example.fpm.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.InputStream;
import java.net.URL;

@Service
public class StorageService {
    @Value("${app.storage.local.baseDir:}")
    private String localBaseDir;

    public String saveToIncoming(MultipartFile file, String overrideFileName) throws IOException {
        if (localBaseDir == null || localBaseDir.trim().isEmpty()) {
            throw new IllegalStateException("local baseDir not configured");
        }
        String name = (overrideFileName != null && !overrideFileName.isBlank()) ? overrideFileName.trim() : file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("file name is required");
        }
        Path incoming = Paths.get(localBaseDir).resolve("incoming");
        if (!Files.exists(incoming)) {
            Files.createDirectories(incoming);
        }
        Path target = incoming.resolve(name);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return target.toString();
    }

     public String saveUrlToIncoming(String fileUrl, String overrideFileName) throws IOException {
         if (localBaseDir == null || localBaseDir.trim().isEmpty()) {
             throw new IllegalStateException("local baseDir not configured");
         }
         if (fileUrl == null || fileUrl.isBlank()) {
             throw new IllegalArgumentException("fileUrl is required");
         }
         String name = (overrideFileName != null && !overrideFileName.isBlank()) ? overrideFileName.trim() : deriveNameFromUrl(fileUrl);
         if (name == null || name.isBlank()) {
             throw new IllegalArgumentException("could not determine file name");
         }
         Path incoming = Paths.get(localBaseDir).resolve("incoming");
         if (!Files.exists(incoming)) {
             Files.createDirectories(incoming);
         }
         Path target = incoming.resolve(name);
         try (InputStream in = new URL(fileUrl).openStream()) {
             Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
         }
         return target.toString();
     }

     private String deriveNameFromUrl(String url) {
         try {
             String path = new URL(url).getPath();
             int idx = path.lastIndexOf('/');
             return idx >= 0 ? path.substring(idx + 1) : path;
         } catch (Exception e) {
             return null;
         }
     }
}
