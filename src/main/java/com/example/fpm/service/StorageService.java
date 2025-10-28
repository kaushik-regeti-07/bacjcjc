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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

     public List<Map<String, Object>> listStorageFiles() throws IOException {
         if (localBaseDir == null || localBaseDir.trim().isEmpty()) {
             throw new IllegalStateException("local baseDir not configured");
         }
         Path storage = Paths.get(localBaseDir).resolve("storage");
         if (!Files.exists(storage)) {
             Files.createDirectories(storage);
         }
         List<Map<String, Object>> out = new ArrayList<>();
         try {
             Files.list(storage)
                     .filter(Files::isRegularFile)
                     .forEach(p -> {
                         Map<String, Object> m = new HashMap<>();
                         m.put("name", p.getFileName().toString());
                         try {
                             m.put("size", Files.size(p));
                             m.put("modified", Files.getLastModifiedTime(p).toMillis());
                         } catch (IOException ignored) {}
                         out.add(m);
                     });
         } catch (IOException e) {
             throw e;
         }
         return out;
     }

     public Map<String, Object> importFromStorage(String fileName) throws IOException {
         if (localBaseDir == null || localBaseDir.trim().isEmpty()) {
             throw new IllegalStateException("local baseDir not configured");
         }
         if (fileName == null || fileName.trim().isEmpty()) {
             throw new IllegalArgumentException("fileName is required");
         }
         Path base = Paths.get(localBaseDir);
         Path storage = base.resolve("storage");
         Path incoming = base.resolve("incoming");
         if (!Files.exists(incoming)) Files.createDirectories(incoming);
         Path src = storage.resolve(fileName);
         Map<String, Object> res = new HashMap<>();
         if (!Files.exists(src)) {
             res.put("imported", false);
             res.put("reason", "file not found in storage");
             return res;
         }
         Path dest = incoming.resolve(fileName);
         Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
         res.put("imported", true);
         res.put("path", dest.toString());
         return res;
     }
}
