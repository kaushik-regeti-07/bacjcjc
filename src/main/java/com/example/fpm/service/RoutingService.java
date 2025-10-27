package com.example.fpm.service;

import com.example.fpm.model.PathConfig;
import com.example.fpm.model.RoutingLog;
import com.example.fpm.repository.PathConfigRepository;
import com.example.fpm.repository.RoutingLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import com.example.fpm.sharepoint.SharePointService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Locale;
import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Collectors;

@Service
public class RoutingService {
    private final PathConfigRepository pathConfigRepository;
    private final RoutingLogRepository routingLogRepository;
    private final SharePointService sharePointService;
    @Value("${app.routing.mode:dry-run}")
    private String routingMode;
    @Value("${app.storage.local.baseDir:}")
    private String localBaseDir;

    public RoutingService(PathConfigRepository pathConfigRepository, RoutingLogRepository routingLogRepository, SharePointService sharePointService) {
        this.pathConfigRepository = pathConfigRepository;
        this.routingLogRepository = routingLogRepository;
        this.sharePointService = sharePointService;
    }

    // Stub: later integrate with Microsoft Graph to process incoming/ folder
    public Map<String, Object> runRoutingNow() {
        if ("local".equalsIgnoreCase(routingMode)) {
            return runLocalRouting();
        } else if ("live".equalsIgnoreCase(routingMode)) {
            List<PathConfig> configs = pathConfigRepository.findAll();
            return sharePointService.runLiveRouting(configs, this::persistLog);
        }
        Map<String, Object> summary = new HashMap<>();
        summary.put("processed", 0);
        summary.put("moved", 0);
        summary.put("skipped", 0);
        summary.put("errors", 0);
        return summary;
    }

    public static class DryRunDecision {
        private String fileName;
        private String prefix;
        private boolean matched;
        private String outputPath; // reports/compliance/<Prefix>
        private String destinationPath; // outputPath + "/" + fileName
        private String reason; // if not matched

        public DryRunDecision() {}

        public DryRunDecision(String fileName, String prefix, boolean matched, String outputPath, String destinationPath, String reason) {
            this.fileName = fileName;
            this.prefix = prefix;
            this.matched = matched;
            this.outputPath = outputPath;
            this.destinationPath = destinationPath;
            this.reason = reason;
        }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix; }
        public boolean isMatched() { return matched; }
        public void setMatched(boolean matched) { this.matched = matched; }
        public String getOutputPath() { return outputPath; }
        public void setOutputPath(String outputPath) { this.outputPath = outputPath; }
        public String getDestinationPath() { return destinationPath; }
        public void setDestinationPath(String destinationPath) { this.destinationPath = destinationPath; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public List<DryRunDecision> dryRunDecisions(List<String> fileNames) {
        List<PathConfig> configs = pathConfigRepository.findAll();
        List<DryRunDecision> decisions = new ArrayList<>();
        for (String name : fileNames) {
            if (name == null || name.trim().isEmpty()) continue;
            String trimmed = name.trim();
            int sep = trimmed.indexOf("__");
            if (sep <= 0) {
                decisions.add(new DryRunDecision(trimmed, null, false, null, null, "Filename missing prefix delimiter '__'"));
                continue;
            }
            String prefix = trimmed.substring(0, sep);
            PathConfig match = null;
            for (PathConfig pc : configs) {
                if (pc.getPrefix() != null && pc.getStatus() != null
                        && pc.getPrefix().equalsIgnoreCase(prefix)
                        && pc.getStatus().equalsIgnoreCase("Active")) {
                    match = pc;
                    break;
                }
            }
            if (match == null) {
                decisions.add(new DryRunDecision(trimmed, prefix, false, null, null, "No active mapping for prefix"));
                continue;
            }
            String outputBase = match.getOutputPath();
            // If outputPath is reports/compliance/Finance we place file inside it
            String destination = (outputBase.endsWith("/")) ? outputBase + trimmed : outputBase + "/" + trimmed;
            decisions.add(new DryRunDecision(trimmed, prefix, true, outputBase, destination, null));
        }
        return decisions;
    }

    private Map<String, Object> runLocalRouting() {
        Map<String, Object> summary = new HashMap<>();
        int processed = 0;
        int moved = 0;
        int skipped = 0;
        int errors = 0;
        if (localBaseDir == null || localBaseDir.trim().isEmpty()) {
            summary.put("processed", processed);
            summary.put("moved", moved);
            summary.put("skipped", skipped);
            summary.put("errors", errors);
            return summary;
        }
        Path base = Paths.get(localBaseDir);
        Path incoming = base.resolve("incoming");
        List<PathConfig> configs = pathConfigRepository.findAll();
        try {
            if (!Files.exists(incoming)) {
                Files.createDirectories(incoming);
            }
            List<Path> files = Files.list(incoming)
                    .filter(p -> Files.isRegularFile(p))
                    .collect(Collectors.toList());
            for (Path file : files) {
                processed++;
                String fileName = file.getFileName().toString();
                int sep = fileName.indexOf("__");
                if (sep <= 0) {
                    skipped++;
                    persistLog(fileName, "SKIPPED", incoming.toString(), "", "Missing prefix delimiter");
                    continue;
                }
                String prefix = fileName.substring(0, sep);
                PathConfig match = null;
                for (PathConfig pc : configs) {
                    if (pc.getPrefix() != null && pc.getStatus() != null
                            && pc.getPrefix().equalsIgnoreCase(prefix)
                            && pc.getStatus().equalsIgnoreCase("Active")) {
                        match = pc;
                        break;
                    }
                }
                if (match == null) {
                    skipped++;
                    persistLog(fileName, "SKIPPED", incoming.toString(), "", "No active mapping for prefix");
                    continue;
                }
                String outRel = match.getOutputPath();
                Path outDir = base.resolve(outRel.replace("/", java.io.File.separator));
                if (!Files.exists(outDir)) {
                    Files.createDirectories(outDir);
                }
                Path target = outDir.resolve(fileName);
                try {
                    Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
                    moved++;
                    persistLog(fileName, "MOVED", incoming.toString(), target.toString(), null);
                } catch (IOException ex) {
                    errors++;
                    persistLog(fileName, "ERROR", incoming.toString(), outDir.toString(), ex.getMessage());
                }
            }
        } catch (IOException e) {
            // ignore here; counts remain
        }
        summary.put("processed", processed);
        summary.put("moved", moved);
        summary.put("skipped", skipped);
        summary.put("errors", errors);
        return summary;
    }

    private void persistLog(String fileName, String action, String from, String to, String message) {
        RoutingLog log = new RoutingLog();
        log.setFileName(fileName);
        log.setAction(action);
        log.setFromPath(from);
        log.setToPath(to);
        log.setMessage(message);
        routingLogRepository.save(log);
    }
}
