package com.example.fpm.controller;

import com.example.fpm.service.StorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

@RestController
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "false")
@RequestMapping("/api/ops")
public class OpsController {

    private final StorageService storageService;

    public OpsController(StorageService storageService) {
        this.storageService = storageService;
    }

    // Upload a file into the local incoming folder (demo/local mode)
    // form-data: file=<binary>, fileName(optional)="Finance__something.pdf"
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importToIncoming(@RequestParam("file") MultipartFile file,
                                                                @RequestParam(value = "fileName", required = false) String fileName) throws Exception {
        String savedPath = storageService.saveToIncoming(file, fileName);
        Map<String, Object> resp = new HashMap<>();
        resp.put("saved", true);
        resp.put("path", savedPath);
        return ResponseEntity.ok(resp);
    }

    // List files under baseDir/storage (for OPS to choose to import)
    @GetMapping("/storage")
    public ResponseEntity<List<Map<String, Object>>> listStorage() throws Exception {
        return ResponseEntity.ok(storageService.listStorageFiles());
    }

    // Import a single file from storage to incoming
    @PostMapping("/import-storage")
    public ResponseEntity<Map<String, Object>> importFromStorage(@RequestParam("fileName") String fileName) throws Exception {
        return ResponseEntity.ok(storageService.importFromStorage(fileName));
    }
}
